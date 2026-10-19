package com.yourbank.wealth.portfolio;

import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientAccess;
import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.portfolio.PortfolioDtos.HistoryPoint;
import com.yourbank.wealth.portfolio.PortfolioDtos.PortfolioDetail;
import com.yourbank.wealth.portfolio.PortfolioDtos.PortfolioSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolios;
    private final PositionRepository positions;
    private final CashAccountRepository cashAccounts;
    private final ClientAccess access;
    private final JdbcClient jdbc;

    public PortfolioService(
            PortfolioRepository portfolios,
            PositionRepository positions,
            CashAccountRepository cashAccounts,
            ClientAccess access,
            JdbcClient jdbc) {
        this.portfolios = portfolios;
        this.positions = positions;
        this.cashAccounts = cashAccounts;
        this.access = access;
        this.jdbc = jdbc;
    }

    /** The portfolio, if this user may see it (404 otherwise, so ids can't be probed). */
    public Portfolio requireVisible(Long portfolioId, CurrentUser user) {
        Portfolio portfolio = portfolios.findById(portfolioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
        if (!access.canSee(portfolio.getClient(), user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found");
        }
        return portfolio;
    }

    public Valuation value(Portfolio portfolio) {
        return Valuation.of(positions.findByPortfolioId(portfolio.getId()), cashAccounts.findByPortfolioId(portfolio.getId()));
    }

    public List<PortfolioSummary> summariesFor(Client client) {
        return portfolios.findByClientId(client.getId()).stream().map(this::summary).toList();
    }

    public List<PortfolioSummary> summariesFor(List<Client> clients) {
        if (clients.isEmpty()) {
            return List.of();
        }
        return portfolios.findByClientIds(clients.stream().map(Client::getId).toList()).stream()
                .map(this::summary)
                .toList();
    }

    public PortfolioSummary summary(Portfolio p) {
        Valuation v = value(p);
        return new PortfolioSummary(
                p.getId(), p.getPortfolioNumber(), p.getName(), p.getServiceType(), p.getCurrency(),
                v.totalValue(), v.cash(), v.cashRatioPercent());
    }

    public PortfolioDetail detail(Long portfolioId, CurrentUser user) {
        Portfolio p = requireVisible(portfolioId, user);
        Valuation v = value(p);
        List<HistoryPoint> history = history(p.getId());
        Client c = p.getClient();
        return new PortfolioDetail(
                p.getId(), p.getPortfolioNumber(), p.getName(), p.getServiceType(), p.getCurrency(), p.getOpenedOn(),
                c.getId(), c.getFullName(), c.getClientNumber(),
                v.totalValue(), v.investedValue(), v.cash(), v.cashRatioPercent(), v.unrealizedPnl(),
                performance(history), v.allocation(), v.lines(), history);
    }

    List<HistoryPoint> history(Long portfolioId) {
        return jdbc.sql("SELECT as_of, index_value FROM valuation_history WHERE portfolio_id = ? ORDER BY as_of")
                .param(portfolioId)
                .query((rs, row) -> new HistoryPoint(rs.getDate("as_of").toLocalDate(), rs.getBigDecimal("index_value")))
                .list();
    }

    /** Change of the performance index over the history shown, in %. */
    static BigDecimal performance(List<HistoryPoint> history) {
        if (history.size() < 2) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal first = history.getFirst().indexValue();
        BigDecimal last = history.getLast().indexValue();
        return last.subtract(first).multiply(BigDecimal.valueOf(100)).divide(first, 2, RoundingMode.HALF_UP);
    }
}
