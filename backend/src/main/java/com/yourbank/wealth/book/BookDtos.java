package com.yourbank.wealth.book;

import com.yourbank.wealth.client.ClientCategory;
import com.yourbank.wealth.mifid.MifidDtos;
import com.yourbank.wealth.portfolio.PortfolioDtos.PortfolioSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** JSON shapes for the RM's book of clients and the client overview. */
public final class BookDtos {

    private BookDtos() {
    }

    public record ClientListItem(
            Long id,
            String clientNumber,
            String fullName,
            ClientCategory category,
            String country,
            int portfolioCount,
            BigDecimal totalValue,
            /** VALID, EXPIRED, PENDING_VALIDATION, IN_PROGRESS or MISSING */
            String mifidStatus,
            Integer riskScore,
            String riskLabel,
            LocalDate mifidValidUntil,
            MifidDtos.NextAction mifidNextAction) {
    }

    public record ClientDetail(
            Long id,
            String clientNumber,
            String firstName,
            String lastName,
            String fullName,
            String email,
            String phone,
            LocalDate birthDate,
            String country,
            ClientCategory category,
            String rmUsername,
            BigDecimal totalValue,
            List<PortfolioSummary> portfolios,
            MifidDtos.Overview mifid) {
    }

    /** The counters on the RM homepage. */
    public record RmHome(
            BigDecimal totalAum,
            int clients,
            int portfolios,
            int mifidPendingValidation,
            int mifidMissingOrExpired,
            int mifidReviewsInProgress,
            int highCashPortfolios,
            int proposalsPendingOver5Days,
            int ordersAwaitingApproval,
            List<ClientListItem> clientsNeedingAttention) {
    }

    /** The counters on the admin (compliance) homepage. */
    public record AdminHome(int ordersAwaitingApproval, int clients, int portfolios) {
    }
}
