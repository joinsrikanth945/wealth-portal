package com.yourbank.wealth.order;

import com.yourbank.wealth.audit.AuditService;
import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientAccess;
import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.instrument.InstrumentRepository;
import com.yourbank.wealth.mifid.MifidProfile;
import com.yourbank.wealth.mifid.MifidService;
import com.yourbank.wealth.order.OrderDtos.OrderDto;
import com.yourbank.wealth.order.OrderDtos.OrderPreview;
import com.yourbank.wealth.order.OrderEnums.LimitType;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.order.OrderEnums.Status;
import com.yourbank.wealth.order.OrderEnums.TimeInForce;
import com.yourbank.wealth.order.PreTradeChecks.Check;
import com.yourbank.wealth.order.PreTradeChecks.Outcome;
import com.yourbank.wealth.portfolio.CashAccount;
import com.yourbank.wealth.portfolio.CashAccountRepository;
import com.yourbank.wealth.portfolio.Portfolio;
import com.yourbank.wealth.portfolio.PortfolioService;
import com.yourbank.wealth.portfolio.Position;
import com.yourbank.wealth.portfolio.PositionRepository;
import com.yourbank.wealth.portfolio.ServiceType;
import com.yourbank.wealth.portfolio.Valuation;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Previews, places and manages orders. Execution is simulated: market orders
 * fill at the last price straight away, limit orders fill if the limit is
 * already reached, stop orders wait.
 */
@Service
public class OrderService {

    private final TradeOrderRepository orders;
    private final InstrumentRepository instruments;
    private final PortfolioService portfolios;
    private final PositionRepository positions;
    private final CashAccountRepository cashAccounts;
    private final MifidService mifid;
    private final ClientAccess access;
    private final AuditService audit;
    private final Clock clock;

    public OrderService(
            TradeOrderRepository orders,
            InstrumentRepository instruments,
            PortfolioService portfolios,
            PositionRepository positions,
            CashAccountRepository cashAccounts,
            MifidService mifid,
            ClientAccess access,
            AuditService audit,
            Clock clock) {
        this.orders = orders;
        this.instruments = instruments;
        this.portfolios = portfolios;
        this.positions = positions;
        this.cashAccounts = cashAccounts;
        this.mifid = mifid;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- preview

    @Transactional(readOnly = true)
    public OrderPreview preview(OrderRequest request, CurrentUser user) {
        Portfolio portfolio = requireTradable(request.portfolioId(), user);
        return evaluate(portfolio, request).preview();
    }

    /** Checks and costs for one order, without saving anything. */
    Evaluation evaluate(Portfolio portfolio, OrderRequest r) {
        validateShape(r);
        Instrument instrument = instruments.findById(r.instrumentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown instrument"));

        BigDecimal price = estimatedPrice(instrument, r);
        OrderCosts costs = OrderCosts.of(instrument, r.side(), r.quantity(), price, r.negotiatedFee());

        Valuation valuation = portfolios.value(portfolio);
        BigDecimal cash = cashAccounts.findByPortfolioIdAndCurrency(portfolio.getId(), instrument.getCurrency())
                .map(CashAccount::getBalance).orElse(BigDecimal.ZERO);
        Optional<Position> position = positions.findOne(portfolio.getId(), instrument.getId());
        BigDecimal held = position.map(Position::getQuantity).orElse(BigDecimal.ZERO);
        BigDecimal positionValue = position
                .map(p -> instrument.valueOf(p.getQuantity(), instrument.getLastPrice()))
                .orElse(BigDecimal.ZERO);
        Optional<MifidProfile> profile = mifid.validProfile(portfolio.getClient());

        List<Check> checks = PreTradeChecks.evaluate(new PreTradeChecks.Context(
                portfolio.getServiceType(), instrument, r.side(), r.quantity(),
                costs.net(), costs.gross(), cash, held, positionValue, valuation.totalValue(),
                profile.map(MifidProfile::getRiskScore).orElse(null),
                profile.map(MifidProfile::isComplexProductsOk).orElse(false)));

        return new Evaluation(portfolio, instrument, r, costs, checks, cash, held);
    }

    /** Just the pre-trade checks for a would-be order (used to vet proposal lines). */
    @Transactional(readOnly = true)
    public List<Check> checksFor(Portfolio portfolio, OrderRequest request) {
        return evaluate(portfolio, request).checks();
    }

    record Evaluation(Portfolio portfolio, Instrument instrument, OrderRequest request, OrderCosts costs,
                      List<Check> checks, BigDecimal cash, BigDecimal held) {

        OrderPreview preview() {
            return new OrderPreview(
                    instrument.getId(), instrument.getName(), instrument.getAssetClass(),
                    OrderDtos.natureLabel(instrument.getAssetClass(), request.side()),
                    instrument.getLastPrice(), costs.price(), costs.gross(), costs.fees(), costs.standardFees(),
                    costs.net(), cash, held.stripTrailingZeros(), checks,
                    !PreTradeChecks.anyFail(checks), PreTradeChecks.anyWarn(checks));
        }
    }

    // ------------------------------------------------------------------ place

    @Transactional
    public OrderDto place(OrderRequest request, CurrentUser user) {
        Portfolio portfolio = requireTradable(request.portfolioId(), user);
        return OrderDto.from(place(portfolio, request, null, user));
    }

    /** Places one order; used directly by proposals. Fails with 400 if any check fails. */
    @Transactional
    public TradeOrder place(Portfolio portfolio, OrderRequest request, Long proposalId, CurrentUser user) {
        Evaluation e = evaluate(portfolio, request);
        if (PreTradeChecks.anyFail(e.checks())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order blocked by pre-trade checks — " + PreTradeChecks.summary(e.checks(), Outcome.FAIL));
        }
        String warnings = PreTradeChecks.summary(e.checks(), Outcome.WARN);
        TradeOrder order = orders.save(new TradeOrder(portfolio, e.instrument(), request, e.costs(),
                warnings, proposalId, user.username(), clock.instant()));
        audit.record(user.username(), "ORDER_CREATED", describe(order) + (warnings == null ? "" : " (needs approval)"));
        if (warnings == null) {
            execute(order);
        }
        return order;
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<OrderDto> list(CurrentUser user, Long portfolioId, Status status) {
        List<TradeOrder> found;
        if (portfolioId != null) {
            portfolios.requireVisible(portfolioId, user);
            found = orders.findByPortfolioIds(List.of(portfolioId));
        } else if (user.isAdmin()) {
            found = orders.findAllWithDetails();
        } else {
            List<Client> clients = user.isRm() ? access.book(user) : List.of(access.self(user));
            List<Long> ids = portfolios.summariesFor(clients).stream().map(s -> s.id()).toList();
            found = ids.isEmpty() ? List.of() : orders.findByPortfolioIds(ids);
        }
        return found.stream()
                .filter(o -> status == null || o.getStatus() == status)
                .map(OrderDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countAwaitingApproval(List<Client> book) {
        List<Long> ids = portfolios.summariesFor(book).stream().map(s -> s.id()).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return orders.findByPortfolioIds(ids).stream().filter(o -> o.getStatus() == Status.PENDING_APPROVAL).count();
    }

    // -------------------------------------------------------------- lifecycle

    @Transactional
    public OrderDto cancel(Long orderId, CurrentUser user) {
        TradeOrder order = requireOrder(orderId, user);
        if (!order.getStatus().isOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only open orders can be cancelled");
        }
        boolean own = order.getCreatedBy().equals(user.username());
        if (!own && !access.canManage(order.getPortfolio().getClient(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't cancel this order");
        }
        order.decide(Status.CANCELLED, user.username(), "Cancelled", clock.instant());
        audit.record(user.username(), "ORDER_CANCELLED", describe(order));
        return OrderDto.from(order);
    }

    /** Compliance approves an order that raised warnings; it then goes to the market. */
    @Transactional
    public OrderDto approve(Long orderId, String note, CurrentUser user) {
        TradeOrder order = requirePendingApproval(orderId, user);
        order.decide(Status.PLACED, user.username(), note == null || note.isBlank() ? "Approved" : note, clock.instant());
        audit.record(user.username(), "ORDER_APPROVED", describe(order));
        execute(order);
        return OrderDto.from(order);
    }

    @Transactional
    public OrderDto reject(Long orderId, String note, CurrentUser user) {
        TradeOrder order = requirePendingApproval(orderId, user);
        order.decide(Status.REJECTED, user.username(), note == null || note.isBlank() ? "Rejected" : note, clock.instant());
        audit.record(user.username(), "ORDER_REJECTED", describe(order));
        return OrderDto.from(order);
    }

    // ---------------------------------------------------------------- helpers

    /** Simulated market: fills now if the price conditions are met, otherwise leaves it placed. */
    void execute(TradeOrder order) {
        Instrument i = order.getInstrument();
        BigDecimal last = i.getLastPrice();
        boolean fills = switch (order.getLimitType()) {
            case MARKET -> true;
            case LIMIT -> order.getSide() == Side.BUY
                    ? order.getLimitPrice().compareTo(last) >= 0
                    : order.getLimitPrice().compareTo(last) <= 0;
            case STOP, STOP_LIMIT -> false;
        };
        if (!fills) {
            order.markPlaced();
            return;
        }
        BigDecimal price = order.getLimitType() == LimitType.LIMIT && order.getSide() == Side.BUY
                ? order.getLimitPrice().min(last)
                : order.getLimitType() == LimitType.LIMIT ? order.getLimitPrice().max(last) : last;
        OrderCosts costs = OrderCosts.of(i, order.getSide(), order.getQuantity(), price, order.getNegotiatedFee());
        Portfolio p = order.getPortfolio();

        CashAccount cash = cashAccounts.findByPortfolioIdAndCurrency(p.getId(), i.getCurrency())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No " + i.getCurrency() + " cash account"));
        Optional<Position> existing = positions.findOne(p.getId(), i.getId());
        if (order.getSide() == Side.BUY) {
            cash.debit(costs.net());
            if (existing.isPresent()) {
                existing.get().buy(order.getQuantity(), price);
            } else {
                positions.save(new Position(p, i, order.getQuantity(), price));
            }
        } else {
            cash.credit(costs.net());
            Position pos = existing.orElseGet(() -> positions.save(new Position(p, i, BigDecimal.ZERO, price)));
            pos.sell(order.getQuantity());
            if (pos.getQuantity().signum() == 0) {
                positions.delete(pos);
            }
        }
        order.fill(price, costs, clock.instant());
        audit.record("system", "ORDER_FILLED", describe(order) + " at " + price.toPlainString());
    }

    /** Who may trade this portfolio: its RM (or an admin), or the client on an execution-only portfolio. */
    private Portfolio requireTradable(Long portfolioId, CurrentUser user) {
        Portfolio portfolio = portfolios.requireVisible(portfolioId, user);
        Client client = portfolio.getClient();
        if (access.canManage(client, user)) {
            return portfolio;
        }
        if (user.isClient() && portfolio.getServiceType() == ServiceType.EXECUTION_ONLY) {
            return portfolio;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Clients can place orders only on execution-only portfolios; ask your relationship manager.");
    }

    private TradeOrder requireOrder(Long orderId, CurrentUser user) {
        TradeOrder order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        portfolios.requireVisible(order.getPortfolio().getId(), user);
        return order;
    }

    private TradeOrder requirePendingApproval(Long orderId, CurrentUser user) {
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only compliance (admin) can decide");
        }
        TradeOrder order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != Status.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not waiting for approval");
        }
        if (order.getCreatedBy().equals(user.username())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't approve your own order (four-eyes rule)");
        }
        return order;
    }

    private static void validateShape(OrderRequest r) {
        if (r.limitType().needsLimitPrice() && r.limitPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A limit price is required for " + r.limitType());
        }
        if (r.limitType().needsStopPrice() && r.stopPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A stop price is required for " + r.limitType());
        }
        if (r.timeInForce() == TimeInForce.GTD
                && (r.validUntil() == null || r.validUntil().isBefore(LocalDate.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Good-till-date orders need a future date");
        }
    }

    /** The price used for estimates: the limit for limit orders, else the last price. */
    static BigDecimal estimatedPrice(Instrument instrument, OrderRequest r) {
        if (r.limitType().needsLimitPrice() && r.limitPrice() != null) {
            return r.limitPrice();
        }
        if (r.limitType() == LimitType.STOP && r.stopPrice() != null) {
            return r.stopPrice();
        }
        return instrument.getLastPrice();
    }

    private static String describe(TradeOrder o) {
        return "#" + o.getId() + " " + o.getSide() + " " + o.getQuantity().stripTrailingZeros().toPlainString()
                + " " + o.getInstrument().getName() + " in " + o.getPortfolio().getPortfolioNumber();
    }
}
