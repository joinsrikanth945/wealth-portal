package com.yourbank.wealth.mifid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yourbank.wealth.TestData;
import com.yourbank.wealth.audit.AuditService;
import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientAccess;
import com.yourbank.wealth.mifid.MifidDtos.FinishRequest;
import com.yourbank.wealth.mifid.MifidDtos.NextAction;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class MifidServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T09:00:00Z");
    private static final String SIGNATURE = "data:image/png;base64,iVBORw0KGgo=";

    private MifidProfileRepository repo;
    private ClientAccess access;
    private MifidService service;
    private Client client;
    private final List<MifidProfile> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repo = mock(MifidProfileRepository.class);
        access = mock(ClientAccess.class);
        service = new MifidService(repo, new MifidQuestionnaire(), access, mock(AuditService.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
        client = TestData.client(1, "rm1", "client1");

        when(access.requireManaged(1L, TestData.RM1)).thenReturn(client);
        when(access.requireVisible(1L, TestData.RM1)).thenReturn(client);
        when(repo.findByClientId(1L)).thenAnswer(inv -> List.copyOf(stored));
        when(repo.save(any(MifidProfile.class))).thenAnswer(inv -> {
            MifidProfile p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", (long) stored.size() + 100);
            stored.addFirst(p);
            return p;
        });
        when(repo.findById(any())).thenAnswer(inv ->
                stored.stream().filter(p -> p.getId().equals(inv.getArgument(0))).findFirst());
    }

    @Test
    void aClientWithoutAProfileNeedsProfiling() {
        assertThat(service.overview(client).nextAction()).isEqualTo(NextAction.PROFILE_NOW);
    }

    @Test
    void startingTwiceReturnsTheSameOpenReview() {
        Long first = service.startReview(1L, TestData.RM1).profile().id();
        Long second = service.startReview(1L, TestData.RM1).profile().id();

        assertThat(second).isEqualTo(first);
        assertThat(service.overview(client).nextAction()).isEqualTo(NextAction.FINALIZE_REVIEW);
    }

    @Test
    void acceptingCreatesAValidProfileForTwelveMonths() {
        Long id = service.startReview(1L, TestData.RM1).profile().id();
        service.saveAnswers(id, MifidQuestionnaireTest.balancedAnswers(), TestData.RM1);

        var result = service.finish(id, new FinishRequest(ClientDecision.ACCEPTED, SIGNATURE), TestData.RM1);

        assertThat(result.profile().status()).isEqualTo(MifidStatus.VALID);
        assertThat(result.profile().riskScore()).isEqualTo(3);
        assertThat(result.profile().validUntil()).isEqualTo(LocalDate.of(2027, 10, 19));
        assertThat(result.profile().signed()).isTrue();
        assertThat(service.overview(client).nextAction()).isEqualTo(NextAction.NONE);
    }

    @Test
    void acceptingANewProfileSupersedesTheOldOne() {
        Long oldId = service.startReview(1L, TestData.RM1).profile().id();
        service.saveAnswers(oldId, MifidQuestionnaireTest.balancedAnswers(), TestData.RM1);
        service.finish(oldId, new FinishRequest(ClientDecision.ACCEPTED, SIGNATURE), TestData.RM1);

        Long newId = service.startReview(1L, TestData.RM1).profile().id();
        service.saveAnswers(newId, MifidQuestionnaireTest.balancedAnswers(), TestData.RM1);
        service.finish(newId, new FinishRequest(ClientDecision.ACCEPTED, SIGNATURE), TestData.RM1);

        Optional<MifidProfile> old = stored.stream().filter(p -> p.getId().equals(oldId)).findFirst();
        assertThat(old).get().extracting(MifidProfile::getStatus).isEqualTo(MifidStatus.SUPERSEDED);
    }

    @Test
    void cannotFinishWithUnansweredQuestions() {
        Long id = service.startReview(1L, TestData.RM1).profile().id();
        service.saveAnswers(id, Map.of("goal", "growth"), TestData.RM1);

        assertThatThrownBy(() -> service.finish(id, new FinishRequest(ClientDecision.ACCEPTED, SIGNATURE), TestData.RM1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unanswered");
    }

    @Test
    void acceptingRequiresASignature() {
        Long id = service.startReview(1L, TestData.RM1).profile().id();
        service.saveAnswers(id, MifidQuestionnaireTest.balancedAnswers(), TestData.RM1);

        assertThatThrownBy(() -> service.finish(id, new FinishRequest(ClientDecision.ACCEPTED, null), TestData.RM1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void underReviewWaitsForTheClient() {
        Long id = service.startReview(1L, TestData.RM1).profile().id();
        service.saveAnswers(id, MifidQuestionnaireTest.balancedAnswers(), TestData.RM1);

        var result = service.finish(id, new FinishRequest(ClientDecision.UNDER_REVIEW, null), TestData.RM1);

        assertThat(result.profile().status()).isEqualTo(MifidStatus.PENDING_VALIDATION);
        assertThat(service.overview(client).current()).isNull();
    }

    @Test
    void rejectsUnknownAnswerOptions() {
        Long id = service.startReview(1L, TestData.RM1).profile().id();

        assertThatThrownBy(() -> service.saveAnswers(id, Map.of("goal", "lottery"), TestData.RM1))
                .isInstanceOf(ResponseStatusException.class);
    }
}
