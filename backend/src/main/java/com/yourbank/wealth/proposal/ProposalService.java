package com.yourbank.wealth.proposal;

import com.yourbank.wealth.audit.AuditService;
import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientAccess;
import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.instrument.InstrumentRepository;
import com.yourbank.wealth.order.OrderCosts;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.order.OrderRequest;
import com.yourbank.wealth.order.OrderService;
import com.yourbank.wealth.order.PreTradeChecks;
import com.yourbank.wealth.order.PreTradeChecks.Check;
import com.yourbank.wealth.order.TradeOrder;
import com.yourbank.wealth.order.TradeOrderRepository;
import com.yourbank.wealth.portfolio.Portfolio;
import com.yourbank.wealth.portfolio.PortfolioService;
import com.yourbank.wealth.portfolio.ServiceType;
import com.yourbank.wealth.proposal.ProposalDtos.LineDto;
import com.yourbank.wealth.proposal.ProposalDtos.ProposalDetail;
import com.yourbank.wealth.proposal.ProposalDtos.ProposalRequest;
import com.yourbank.wealth.proposal.ProposalDtos.ProposalSummary;
import com.yourbank.wealth.shared.pdf.PdfBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Investment proposals: WORKING -> SENT_FOR_VALIDATION -> VALIDATED (client
 * signs) -> EXECUTED (orders placed), or REJECTED by the client.
 * Only for advisory portfolios.
 */
@Service
public class ProposalService {

    static final Duration PENDING_ALERT = Duration.ofDays(5);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.of("Europe/Paris"));

    private final ProposalRepository proposals;
    private final InstrumentRepository instruments;
    private final PortfolioService portfolios;
    private final OrderService orderService;
    private final TradeOrderRepository orders;
    private final ClientAccess access;
    private final AuditService audit;
    private final Clock clock;

    public ProposalService(
            ProposalRepository proposals,
            InstrumentRepository instruments,
            PortfolioService portfolios,
            OrderService orderService,
            TradeOrderRepository orders,
            ClientAccess access,
            AuditService audit,
            Clock clock) {
        this.proposals = proposals;
        this.instruments = instruments;
        this.portfolios = portfolios;
        this.orderService = orderService;
        this.orders = orders;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<ProposalSummary> list(CurrentUser user, Long portfolioId) {
        List<Long> ids;
        if (portfolioId != null) {
            portfolios.requireVisible(portfolioId, user);
            ids = List.of(portfolioId);
        } else {
            List<Client> clients = user.isClient() && !user.isRm() && !user.isAdmin()
                    ? List.of(access.self(user))
                    : access.book(user);
            ids = portfolios.summariesFor(clients).stream().map(s -> s.id()).toList();
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Proposal> found = proposals.findByPortfolioIds(ids);
        // Clients don't see their RM's drafts.
        boolean clientView = user.isClient() && !user.isRm() && !user.isAdmin();
        return found.stream()
                .filter(p -> !clientView || p.getStatus() != ProposalStatus.WORKING)
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProposalDetail get(Long id, CurrentUser user) {
        return detail(requireVisible(id, user));
    }

    @Transactional(readOnly = true)
    public long countPendingOver5Days(List<Client> book) {
        List<Long> ids = portfolios.summariesFor(book).stream().map(s -> s.id()).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return proposals.findByPortfolioIds(ids).stream().filter(this::pendingTooLong).count();
    }

    // ----------------------------------------------------------------- writes

    @Transactional
    public ProposalDetail create(ProposalRequest request, CurrentUser user) {
        Portfolio portfolio = requireAdvisoryManaged(request.portfolioId(), user);
        Proposal proposal = new Proposal(portfolio, request.title(), request.rationale(), user.username(), clock.instant());
        proposal.edit(request.title(), request.rationale(), lineSpecs(request), clock.instant());
        proposals.save(proposal);
        audit.record(user.username(), "PROPOSAL_CREATED", "#" + proposal.getId() + " for " + portfolio.getPortfolioNumber());
        return detail(proposal);
    }

    @Transactional
    public ProposalDetail update(Long id, ProposalRequest request, CurrentUser user) {
        Proposal proposal = requireManaged(id, user);
        if (proposal.getStatus() != ProposalStatus.WORKING && proposal.getStatus() != ProposalStatus.SENT_FOR_VALIDATION) {
            throw conflict("Only working or sent proposals can be edited");
        }
        if (!proposal.getPortfolio().getId().equals(request.portfolioId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A proposal can't move to another portfolio");
        }
        proposal.edit(request.title(), request.rationale(), lineSpecs(request), clock.instant());
        audit.record(user.username(), "PROPOSAL_EDITED", "#" + id);
        return detail(proposal);
    }

    @Transactional
    public void delete(Long id, CurrentUser user) {
        Proposal proposal = requireManaged(id, user);
        if (proposal.getStatus() != ProposalStatus.WORKING) {
            throw conflict("Only working proposals can be deleted");
        }
        proposals.delete(proposal);
        audit.record(user.username(), "PROPOSAL_DELETED", "#" + id);
    }

    /** Sends to the client. Lines whose checks FAIL must be fixed first. */
    @Transactional
    public ProposalDetail send(Long id, CurrentUser user) {
        Proposal proposal = requireManaged(id, user);
        if (proposal.getStatus() != ProposalStatus.WORKING) {
            throw conflict("Only working proposals can be sent");
        }
        ProposalDetail current = detail(proposal);
        if (current.hasBlockingChecks()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Some lines fail the pre-trade checks; fix them before sending");
        }
        proposal.send(clock.instant());
        audit.record(user.username(), "PROPOSAL_SENT", "#" + id);
        return detail(proposal);
    }

    /** The client accepts and signs (or the RM records the client's signature in a meeting). */
    @Transactional
    public ProposalDetail validate(Long id, String signature, CurrentUser user) {
        Proposal proposal = requireVisible(id, user);
        requireDecider(proposal, user);
        if (proposal.getStatus() != ProposalStatus.SENT_FOR_VALIDATION) {
            throw conflict("Only proposals sent for validation can be accepted");
        }
        if (signature == null || !signature.startsWith("data:image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The client's signature is required");
        }
        proposal.validate(signature, user.username(), clock.instant());
        audit.record(user.username(), "PROPOSAL_VALIDATED", "#" + id);
        return detail(proposal);
    }

    @Transactional
    public ProposalDetail reject(Long id, String reason, CurrentUser user) {
        Proposal proposal = requireVisible(id, user);
        requireDecider(proposal, user);
        if (proposal.getStatus() != ProposalStatus.SENT_FOR_VALIDATION) {
            throw conflict("Only proposals sent for validation can be rejected");
        }
        proposal.reject(reason, user.username(), clock.instant());
        audit.record(user.username(), "PROPOSAL_REJECTED", "#" + id);
        return detail(proposal);
    }

    /** Turns an accepted proposal into market orders. */
    @Transactional
    public ProposalDetail execute(Long id, CurrentUser user) {
        Proposal proposal = requireManaged(id, user);
        if (proposal.getStatus() != ProposalStatus.VALIDATED) {
            throw conflict("Only accepted proposals can be executed");
        }
        Portfolio portfolio = proposal.getPortfolio();
        for (ProposalLine line : proposal.getLines()) {
            orderService.place(portfolio, OrderRequest.market(portfolio.getId(), line.getInstrument().getId(),
                    line.getSide(), line.getQuantity()), proposal.getId(), user);
        }
        proposal.markExecuted(clock.instant());
        audit.record(user.username(), "PROPOSAL_EXECUTED", "#" + id);
        return detail(proposal);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long id, CurrentUser user) {
        Proposal p = requireVisible(id, user);
        ProposalDetail d = detail(p);
        ProposalSummary s = d.summary();
        PdfBuilder pdf = new PdfBuilder("Investment proposal", s.title());
        pdf.keyValues(List.of(
                new String[] {"Client", s.clientName()},
                new String[] {"Portfolio", s.portfolioNumber() + " - " + s.portfolioName()},
                new String[] {"Prepared by", s.createdBy()},
                new String[] {"Date", DATE.format(s.createdAt())},
                new String[] {"Status", s.status().name().replace('_', ' ')}));
        pdf.heading("Our recommendation").paragraph(d.rationale());
        List<String[]> rows = new ArrayList<>();
        for (LineDto l : d.lines()) {
            rows.add(new String[] {l.side().name(), l.instrumentName(), l.instrumentCode(),
                    plain(l.quantity()), money(l.lastPrice()), money(l.estimatedAmount()), money(l.estimatedFees())});
        }
        pdf.heading("Proposed transactions").table(
                new String[] {"Side", "Instrument", "Code", "Quantity", "Price", "Amount", "Est. fees"},
                new float[] {0.08f, 0.32f, 0.18f, 0.1f, 0.1f, 0.12f, 0.1f},
                new boolean[] {false, false, false, true, true, true, true}, rows);
        pdf.total("Estimated purchases", money(s.estimatedBuys()));
        pdf.heading("Costs and charges (ex-ante)").paragraph(
                "Estimated transaction costs are shown per line. No product costs are charged by the bank on top of "
                        + "each fund's own ongoing charges. Past performance is not a reliable indicator of future results.");
        pdf.heading("Suitability statement").paragraph(
                "This proposal was checked against the client's MiFID II suitability profile, the portfolio's "
                        + "concentration limits and the appropriateness of any complex products before it was issued.");
        if (p.getSignaturePng() != null) {
            pdf.signature("Accepted and signed by the client on " + DATE.format(p.getDecidedAt()), p.getSignaturePng());
        }
        return pdf.toBytes();
    }

    // ---------------------------------------------------------------- helpers

    private ProposalSummary summary(Proposal p) {
        BigDecimal buys = BigDecimal.ZERO;
        BigDecimal sells = BigDecimal.ZERO;
        for (ProposalLine l : p.getLines()) {
            BigDecimal value = l.getInstrument().valueOf(l.getQuantity(), l.getInstrument().getLastPrice());
            if (l.getSide() == Side.BUY) {
                buys = buys.add(value);
            } else {
                sells = sells.add(value);
            }
        }
        Portfolio pf = p.getPortfolio();
        return new ProposalSummary(
                p.getId(), p.getTitle(), p.getStatus(), pf.getId(), pf.getPortfolioNumber(), pf.getName(),
                pf.getClient().getId(), pf.getClient().getFullName(), p.getLines().size(),
                buys.setScale(2, RoundingMode.HALF_UP), sells.setScale(2, RoundingMode.HALF_UP),
                p.getCreatedBy(), p.getCreatedAt(), p.getSentAt(), pendingTooLong(p));
    }

    private ProposalDetail detail(Proposal p) {
        boolean open = p.getStatus() == ProposalStatus.WORKING || p.getStatus() == ProposalStatus.SENT_FOR_VALIDATION;
        List<LineDto> lines = new ArrayList<>();
        boolean blocking = false;
        boolean warnings = false;
        for (ProposalLine l : p.getLines()) {
            Instrument i = l.getInstrument();
            OrderCosts costs = OrderCosts.of(i, l.getSide(), l.getQuantity(), i.getLastPrice(), null);
            List<Check> checks = open
                    ? orderService.checksFor(p.getPortfolio(), OrderRequest.market(p.getPortfolio().getId(), i.getId(), l.getSide(), l.getQuantity()))
                    : List.of();
            blocking |= PreTradeChecks.anyFail(checks);
            warnings |= PreTradeChecks.anyWarn(checks);
            lines.add(new LineDto(l.getId(), l.getLineNo(), i.getId(), i.getCode(), i.getName(), i.getAssetClass(),
                    l.getSide(), l.getQuantity().stripTrailingZeros(), i.getLastPrice(), costs.gross(), costs.fees(),
                    l.getComment(), checks));
        }
        List<Long> orderIds = p.getId() == null ? List.of() : orders.findByPortfolioIds(List.of(p.getPortfolio().getId())).stream()
                .filter(o -> p.getId().equals(o.getProposalId()))
                .map(TradeOrder::getId)
                .toList();
        return new ProposalDetail(summary(p), p.getRationale(), p.getDecidedAt(), p.getDecidedBy(), p.getRejectReason(),
                p.getSignaturePng() != null, lines, blocking, warnings, orderIds);
    }

    private boolean pendingTooLong(Proposal p) {
        return p.getStatus() == ProposalStatus.SENT_FOR_VALIDATION && p.getSentAt() != null
                && p.getSentAt().plus(PENDING_ALERT).isBefore(clock.instant());
    }

    private List<Proposal.LineSpec> lineSpecs(ProposalRequest r) {
        return r.lines().stream().map(l -> new Proposal.LineSpec(
                instruments.findById(l.instrumentId()).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown instrument " + l.instrumentId())),
                l.side(), l.quantity(), l.comment())).toList();
    }

    private Portfolio requireAdvisoryManaged(Long portfolioId, CurrentUser user) {
        Portfolio portfolio = portfolios.requireVisible(portfolioId, user);
        if (!access.canManage(portfolio.getClient(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the client's relationship manager can do this");
        }
        if (portfolio.getServiceType() != ServiceType.ADVISORY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proposals are only for advisory portfolios");
        }
        return portfolio;
    }

    private Proposal requireVisible(Long id, CurrentUser user) {
        Proposal p = proposals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));
        portfolios.requireVisible(p.getPortfolio().getId(), user);
        boolean clientView = user.isClient() && !user.isRm() && !user.isAdmin();
        if (clientView && p.getStatus() == ProposalStatus.WORKING) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found");
        }
        return p;
    }

    private Proposal requireManaged(Long id, CurrentUser user) {
        Proposal p = requireVisible(id, user);
        if (!access.canManage(p.getPortfolio().getClient(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the client's relationship manager can do this");
        }
        return p;
    }

    /** The client themselves, or their RM recording the client's decision. */
    private void requireDecider(Proposal p, CurrentUser user) {
        Client c = p.getPortfolio().getClient();
        boolean self = user.isClient() && user.username().equals(c.getPortalUsername());
        if (!self && !access.canManage(c, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the client or their RM can decide");
        }
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static String money(BigDecimal v) {
        return v == null ? "" : String.format(Locale.ENGLISH, "%,.2f", v);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
