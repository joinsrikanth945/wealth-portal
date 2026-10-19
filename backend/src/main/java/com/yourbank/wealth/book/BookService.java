package com.yourbank.wealth.book;

import com.yourbank.wealth.book.BookDtos.ClientDetail;
import com.yourbank.wealth.book.BookDtos.ClientListItem;
import com.yourbank.wealth.book.BookDtos.AdminHome;
import com.yourbank.wealth.book.BookDtos.RmHome;
import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientAccess;
import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.mifid.MifidDtos;
import com.yourbank.wealth.mifid.MifidDtos.NextAction;
import com.yourbank.wealth.mifid.MifidService;
import com.yourbank.wealth.order.OrderEnums;
import com.yourbank.wealth.order.OrderService;
import com.yourbank.wealth.proposal.ProposalService;
import com.yourbank.wealth.portfolio.PortfolioDtos.PortfolioSummary;
import com.yourbank.wealth.portfolio.PortfolioService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The RM's view across clients: lists, client pages and homepage counters. */
@Service
@Transactional(readOnly = true)
public class BookService {

    /** Cash above this share of a portfolio is flagged on the RM homepage. */
    static final BigDecimal HIGH_CASH_PERCENT = BigDecimal.valueOf(30);

    private final ClientAccess access;
    private final PortfolioService portfolios;
    private final MifidService mifid;
    private final ProposalService proposals;
    private final OrderService orders;

    public BookService(ClientAccess access, PortfolioService portfolios, MifidService mifid,
                       ProposalService proposals, OrderService orders) {
        this.access = access;
        this.portfolios = portfolios;
        this.mifid = mifid;
        this.proposals = proposals;
        this.orders = orders;
    }

    public List<ClientListItem> clients(CurrentUser user, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return access.book(user).stream()
                .filter(c -> q.isEmpty()
                        || c.getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || c.getClientNumber().toLowerCase(Locale.ROOT).contains(q))
                .map(this::listItem)
                .toList();
    }

    public ClientDetail client(Long clientId, CurrentUser user) {
        return detail(access.requireVisible(clientId, user));
    }

    /** For a signed-in client: their own record. */
    public ClientDetail self(CurrentUser user) {
        return detail(access.self(user));
    }

    public RmHome home(CurrentUser user) {
        List<Client> book = access.book(user);
        List<ClientListItem> items = book.stream().map(this::listItem).toList();
        List<PortfolioSummary> all = portfolios.summariesFor(book);

        BigDecimal aum = all.stream().map(PortfolioSummary::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        int highCash = (int) all.stream().filter(p -> p.cashRatioPercent().compareTo(HIGH_CASH_PERCENT) > 0).count();
        int pending = count(items, "PENDING_VALIDATION");
        int inProgress = count(items, "IN_PROGRESS");
        int missingOrExpired = count(items, "MISSING") + count(items, "EXPIRED");

        List<ClientListItem> attention = items.stream()
                .filter(i -> i.mifidNextAction() != NextAction.NONE)
                .toList();

        return new RmHome(aum, book.size(), all.size(), pending, missingOrExpired, inProgress, highCash,
                (int) proposals.countPendingOver5Days(book), (int) orders.countAwaitingApproval(book), attention);
    }

    public AdminHome adminHome(CurrentUser user) {
        List<Client> all = access.book(user);
        int awaiting = orders.list(user, null, OrderEnums.Status.PENDING_APPROVAL).size();
        return new AdminHome(awaiting, all.size(), portfolios.summariesFor(all).size());
    }

    private static int count(List<ClientListItem> items, String status) {
        return (int) items.stream().filter(i -> status.equals(i.mifidStatus())).count();
    }

    private ClientListItem listItem(Client c) {
        List<PortfolioSummary> ps = portfolios.summariesFor(c);
        MifidDtos.Overview m = mifid.overview(c);
        MifidDtos.ProfileSnapshot shown = m.openReview() != null ? m.openReview() : m.current();
        String status = shown == null ? "MISSING" : shown.displayStatus();
        MifidDtos.ProfileSnapshot current = m.current();
        return new ClientListItem(
                c.getId(), c.getClientNumber(), c.getFullName(), c.getCategory(), c.getCountry(),
                ps.size(), total(ps), status,
                current == null ? null : current.riskScore(),
                current == null ? null : current.riskLabel(),
                current == null ? null : current.validUntil(),
                m.nextAction());
    }

    private ClientDetail detail(Client c) {
        List<PortfolioSummary> ps = portfolios.summariesFor(c);
        return new ClientDetail(
                c.getId(), c.getClientNumber(), c.getFirstName(), c.getLastName(), c.getFullName(),
                c.getEmail(), c.getPhone(), c.getBirthDate(), c.getCountry(), c.getCategory(), c.getRmUsername(),
                total(ps), ps, mifid.overview(c));
    }

    private static BigDecimal total(List<PortfolioSummary> ps) {
        return ps.stream().map(PortfolioSummary::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
