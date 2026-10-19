package com.yourbank.wealth.mifid;

import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.mifid.MifidDtos.FinishRequest;
import com.yourbank.wealth.mifid.MifidDtos.Overview;
import com.yourbank.wealth.mifid.MifidDtos.ReviewDetail;
import com.yourbank.wealth.mifid.MifidDtos.SaveAnswersRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "MiFID", description = "Suitability questionnaire, risk profile and review workflow")
public class MifidController {

    private final MifidService mifid;
    private final MifidQuestionnaire questionnaire;

    public MifidController(MifidService mifid, MifidQuestionnaire questionnaire) {
        this.mifid = mifid;
        this.questionnaire = questionnaire;
    }

    @GetMapping("/api/mifid/questionnaire")
    @Operation(summary = "The current questionnaire: sections, questions and answer options")
    public MifidQuestionnaire.Definition definition() {
        return questionnaire.definition();
    }

    @GetMapping("/api/clients/{clientId}/mifid")
    @Operation(summary = "A client's current profile, open review, next action and history")
    public Overview overview(@PathVariable Long clientId, @AuthenticationPrincipal Jwt jwt) {
        return mifid.overview(clientId, CurrentUser.from(jwt));
    }

    @PostMapping("/api/rm/clients/{clientId}/mifid/reviews")
    @Operation(summary = "Start a review (returns the open one if there already is one)")
    public ReviewDetail start(@PathVariable Long clientId, @AuthenticationPrincipal Jwt jwt) {
        return mifid.startReview(clientId, CurrentUser.from(jwt));
    }

    @GetMapping("/api/rm/mifid/reviews/{reviewId}")
    @Operation(summary = "A review with its answers and live score")
    public ReviewDetail review(@PathVariable Long reviewId, @AuthenticationPrincipal Jwt jwt) {
        return mifid.review(reviewId, CurrentUser.from(jwt));
    }

    @PutMapping("/api/rm/mifid/reviews/{reviewId}/answers")
    @Operation(summary = "Save answers (partial saves allowed): { \"answers\": { \"goal\": \"growth\" } }")
    public ReviewDetail saveAnswers(
            @PathVariable Long reviewId,
            @Valid @RequestBody SaveAnswersRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return mifid.saveAnswers(reviewId, request.answers(), CurrentUser.from(jwt));
    }

    @PostMapping("/api/rm/mifid/reviews/{reviewId}/finish")
    @Operation(summary = "Finish with the client's decision: ACCEPTED (needs signature), UNDER_REVIEW or REFUSED")
    public ReviewDetail finish(
            @PathVariable Long reviewId,
            @Valid @RequestBody FinishRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return mifid.finish(reviewId, request, CurrentUser.from(jwt));
    }

    @PostMapping("/api/rm/mifid/reviews/{reviewId}/cancel")
    @Operation(summary = "Cancel an open review; the current profile is unchanged")
    public ReviewDetail cancel(@PathVariable Long reviewId, @AuthenticationPrincipal Jwt jwt) {
        return mifid.cancel(reviewId, CurrentUser.from(jwt));
    }
}
