package com.yourbank.wealth.proposal;

import com.yourbank.wealth.instrument.AssetClass;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.order.PreTradeChecks.Check;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ProposalDtos {

    private ProposalDtos() {
    }

    public record LineRequest(
            @NotNull Long instrumentId,
            @NotNull Side side,
            @NotNull @Positive BigDecimal quantity,
            @Size(max = 500) String comment) {
    }

    public record ProposalRequest(
            @NotNull Long portfolioId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String rationale,
            @NotEmpty @Valid List<LineRequest> lines) {
    }

    public record SignRequest(@Size(max = 600_000) String signature) {
    }

    public record RejectRequest(@Size(max = 500) String reason) {
    }

    public record LineDto(
            Long id,
            int lineNo,
            Long instrumentId,
            String instrumentCode,
            String instrumentName,
            AssetClass assetClass,
            Side side,
            BigDecimal quantity,
            BigDecimal lastPrice,
            BigDecimal estimatedAmount,
            BigDecimal estimatedFees,
            String comment,
            /** Pre-trade checks as if the line were traded now (empty once executed). */
            List<Check> checks) {
    }

    public record ProposalSummary(
            Long id,
            String title,
            ProposalStatus status,
            Long portfolioId,
            String portfolioNumber,
            String portfolioName,
            Long clientId,
            String clientName,
            int lineCount,
            BigDecimal estimatedBuys,
            BigDecimal estimatedSells,
            String createdBy,
            Instant createdAt,
            Instant sentAt,
            boolean pendingOver5Days) {
    }

    public record ProposalDetail(
            ProposalSummary summary,
            String rationale,
            Instant decidedAt,
            String decidedBy,
            String rejectReason,
            boolean signed,
            List<LineDto> lines,
            boolean hasBlockingChecks,
            boolean hasWarnings,
            List<Long> orderIds) {
    }
}
