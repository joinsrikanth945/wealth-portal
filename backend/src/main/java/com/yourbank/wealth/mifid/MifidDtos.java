package com.yourbank.wealth.mifid;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** JSON shapes for the MiFID endpoints. */
public final class MifidDtos {

    private MifidDtos() {
    }

    /** What the RM should do next for this client. */
    public enum NextAction {
        PROFILE_NOW,      // no profile yet
        REVIEW_NOW,       // profile expired or expiring within 30 days
        FINALIZE_REVIEW,  // a review is open
        NONE
    }

    /**
     * One profile or review. {@code displayStatus} is the stored status, except
     * that a VALID profile past its end date shows as EXPIRED.
     */
    public record ProfileSnapshot(
            Long id,
            MifidStatus status,
            String displayStatus,
            Integer riskScore,
            String riskLabel,
            boolean complexProductsOk,
            int completionPercent,
            ClientDecision clientDecision,
            LocalDate validUntil,
            String createdBy,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            boolean signed) {
    }

    public record Overview(
            Long clientId,
            String clientName,
            ProfileSnapshot current,
            ProfileSnapshot openReview,
            NextAction nextAction,
            List<ProfileSnapshot> history) {
    }

    public record ReviewDetail(
            ProfileSnapshot profile,
            Long clientId,
            String clientName,
            String questionnaireVersion,
            Map<String, String> answers,
            MifidQuestionnaire.Score score) {
    }

    public record SaveAnswersRequest(@NotNull Map<String, String> answers) {
    }

    public record FinishRequest(
            @NotNull ClientDecision decision,
            /** PNG data URL from the signature pad; required when the decision is ACCEPTED. */
            @Size(max = 600_000) String signature) {
    }
}
