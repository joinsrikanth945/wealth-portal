package com.yourbank.wealth.mifid;

import com.yourbank.wealth.audit.AuditService;
import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientAccess;
import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.mifid.MifidDtos.FinishRequest;
import com.yourbank.wealth.mifid.MifidDtos.NextAction;
import com.yourbank.wealth.mifid.MifidDtos.Overview;
import com.yourbank.wealth.mifid.MifidDtos.ProfileSnapshot;
import com.yourbank.wealth.mifid.MifidDtos.ReviewDetail;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * MiFID suitability profiling: start a review, save answers step by step,
 * then finish with the client's decision and signature.
 */
@Service
public class MifidService {

    /** How long an accepted profile stays valid. */
    static final int VALIDITY_MONTHS = 12;

    private final MifidProfileRepository profiles;
    private final MifidQuestionnaire questionnaire;
    private final ClientAccess access;
    private final AuditService audit;
    private final Clock clock;

    public MifidService(
            MifidProfileRepository profiles,
            MifidQuestionnaire questionnaire,
            ClientAccess access,
            AuditService audit,
            Clock clock) {
        this.profiles = profiles;
        this.questionnaire = questionnaire;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public Overview overview(Long clientId, CurrentUser user) {
        return overview(access.requireVisible(clientId, user));
    }

    @Transactional(readOnly = true)
    public Overview overview(Client client) {
        List<MifidProfile> all = profiles.findByClientId(client.getId());
        LocalDate today = LocalDate.now(clock);

        Optional<MifidProfile> current = currentProfile(all);
        Optional<MifidProfile> open = all.stream().filter(MifidProfile::isOpenReview).findFirst();

        NextAction next;
        if (open.isPresent()) {
            next = NextAction.FINALIZE_REVIEW;
        } else if (current.isEmpty()) {
            next = NextAction.PROFILE_NOW;
        } else if (current.get().getValidUntil() != null
                && current.get().getValidUntil().isBefore(today.plusDays(30))) {
            next = NextAction.REVIEW_NOW;
        } else {
            next = NextAction.NONE;
        }

        return new Overview(
                client.getId(), client.getFullName(),
                current.map(p -> snapshot(p, today)).orElse(null),
                open.map(p -> snapshot(p, today)).orElse(null),
                next,
                all.stream().map(p -> snapshot(p, today)).toList());
    }

    /** The profile in force (latest accepted one), even if it has expired. */
    static Optional<MifidProfile> currentProfile(List<MifidProfile> all) {
        return all.stream()
                .filter(p -> p.getStatus() == MifidStatus.VALID)
                .max(Comparator.comparing(MifidProfile::getCompletedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    /** The client's valid, unexpired profile, used by order checks. */
    @Transactional(readOnly = true)
    public Optional<MifidProfile> validProfile(Client client) {
        LocalDate today = LocalDate.now(clock);
        return currentProfile(profiles.findByClientId(client.getId())).filter(p -> !p.isExpired(today));
    }

    @Transactional(readOnly = true)
    public ReviewDetail review(Long profileId, CurrentUser user) {
        MifidProfile profile = requireProfile(profileId, user, false);
        return detail(profile);
    }

    // ----------------------------------------------------------------- writes

    /** Starts a review, or returns the one already open. */
    @Transactional
    public ReviewDetail startReview(Long clientId, CurrentUser user) {
        Client client = access.requireManaged(clientId, user);
        Optional<MifidProfile> open = profiles.findByClientId(clientId).stream()
                .filter(MifidProfile::isOpenReview)
                .findFirst();
        if (open.isPresent()) {
            return detail(open.get());
        }
        MifidProfile created = profiles.save(MifidProfile.startReview(client, user.username(), clock.instant()));
        audit.record(user.username(), "MIFID_REVIEW_STARTED", "Client " + client.getClientNumber());
        return detail(created);
    }

    /** Saves some or all answers. Unknown questions or options are rejected. */
    @Transactional
    public ReviewDetail saveAnswers(Long profileId, Map<String, String> answers, CurrentUser user) {
        MifidProfile profile = requireOpenReview(profileId, user);
        answers.forEach((questionId, optionId) -> {
            if (!questionnaire.isValidAnswer(questionId, optionId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown answer '" + optionId + "' for question '" + questionId + "'");
            }
        });
        Map<String, String> merged = new java.util.HashMap<>(profile.getAnswers());
        merged.putAll(answers);
        profile.updateAnswers(answers, questionnaire.score(merged).completionPercent(), clock.instant());
        return detail(profile);
    }

    /** Finishes the review with the client's decision. Every question must be answered. */
    @Transactional
    public ReviewDetail finish(Long profileId, FinishRequest request, CurrentUser user) {
        MifidProfile profile = requireOpenReview(profileId, user);
        MifidQuestionnaire.Score score = questionnaire.score(profile.getAnswers());
        if (!score.complete()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unanswered questions: " + String.join(", ", score.missingQuestions()));
        }
        if (request.decision() == ClientDecision.ACCEPTED
                && (request.signature() == null || !request.signature().startsWith("data:image/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The client's signature is required to accept");
        }

        Instant now = clock.instant();
        if (request.decision() == ClientDecision.ACCEPTED) {
            profiles.findByClientId(profile.getClient().getId()).stream()
                    .filter(p -> p.getStatus() == MifidStatus.VALID && !p.getId().equals(profile.getId()))
                    .forEach(p -> p.supersede(now));
        }
        profile.complete(score, request.decision(), request.signature(),
                LocalDate.now(clock).plusMonths(VALIDITY_MONTHS), now);

        audit.record(user.username(), "MIFID_REVIEW_" + request.decision(),
                "Client " + profile.getClient().getClientNumber() + ", risk score " + score.riskScore());
        return detail(profile);
    }

    @Transactional
    public ReviewDetail cancel(Long profileId, CurrentUser user) {
        MifidProfile profile = requireOpenReview(profileId, user);
        profile.cancel(clock.instant());
        audit.record(user.username(), "MIFID_REVIEW_CANCELLED", "Client " + profile.getClient().getClientNumber());
        return detail(profile);
    }

    // ---------------------------------------------------------------- helpers

    private MifidProfile requireProfile(Long profileId, CurrentUser user, boolean manage) {
        MifidProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (manage) {
            access.requireManaged(profile.getClient().getId(), user);
        } else {
            access.requireVisible(profile.getClient().getId(), user);
        }
        return profile;
    }

    private MifidProfile requireOpenReview(Long profileId, CurrentUser user) {
        MifidProfile profile = requireProfile(profileId, user, true);
        if (!profile.isOpenReview()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This review is already closed (" + profile.getStatus() + ")");
        }
        return profile;
    }

    private ReviewDetail detail(MifidProfile p) {
        return new ReviewDetail(
                snapshot(p, LocalDate.now(clock)),
                p.getClient().getId(),
                p.getClient().getFullName(),
                p.getQuestionnaireVersion(),
                Map.copyOf(p.getAnswers()),
                questionnaire.score(p.getAnswers()));
    }

    static ProfileSnapshot snapshot(MifidProfile p, LocalDate today) {
        Integer score = p.getRiskScore();
        return new ProfileSnapshot(
                p.getId(),
                p.getStatus(),
                p.isExpired(today) ? "EXPIRED" : p.getStatus().name(),
                score,
                score == null ? null : MifidQuestionnaire.RISK_LABELS.get(score - 1),
                p.isComplexProductsOk(),
                p.getCompletionPercent(),
                p.getClientDecision(),
                p.getValidUntil(),
                p.getCreatedBy(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getCompletedAt(),
                p.getSignaturePng() != null);
    }
}
