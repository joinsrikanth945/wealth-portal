package com.yourbank.wealth.mifid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MifidQuestionnaireTest {

    private final MifidQuestionnaire questionnaire = new MifidQuestionnaire();

    static Map<String, String> balancedAnswers() {
        Map<String, String> a = new HashMap<>();
        a.put("goal", "growth");          // 3
        a.put("horizon", "y5to10");       // 3
        a.put("income", "50to100");       // 2
        a.put("share", "25to50");         // 3
        a.put("reserve", "3to6m");        // 3
        a.put("drawdown", "hold");        // 3
        a.put("maxloss", "l10");          // 2  -> 19 points
        a.put("exp_equities", "some");
        a.put("exp_derivatives", "none");
        a.put("leverage_quiz", "unsure");
        return a;
    }

    @Test
    void scoresACompleteBalancedProfile() {
        MifidQuestionnaire.Score score = questionnaire.score(balancedAnswers());

        assertThat(score.points()).isEqualTo(19);
        assertThat(score.riskScore()).isEqualTo(3);
        assertThat(score.riskLabel()).isEqualTo("Balanced");
        assertThat(score.complexProductsOk()).isFalse();
        assertThat(score.complete()).isTrue();
        assertThat(score.completionPercent()).isEqualTo(100);
    }

    @Test
    void allowsComplexProductsOnlyWithExperienceAndTheRightQuizAnswer() {
        Map<String, String> a = balancedAnswers();
        a.put("exp_derivatives", "regular");
        assertThat(questionnaire.score(a).complexProductsOk()).isFalse(); // still "unsure"

        a.put("leverage_quiz", "false");
        assertThat(questionnaire.score(a).complexProductsOk()).isTrue();
    }

    @Test
    void partialAnswersGiveNoRiskScoreButShowProgress() {
        MifidQuestionnaire.Score score = questionnaire.score(Map.of("goal", "growth", "horizon", "gt10"));

        assertThat(score.riskScore()).isNull();
        assertThat(score.completionPercent()).isEqualTo(20);
        assertThat(score.missingQuestions()).contains("income", "leverage_quiz").doesNotContain("goal");
    }

    @ParameterizedTest
    @CsvSource({"7,1", "11,1", "12,2", "15,2", "16,3", "19,3", "20,4", "23,4", "24,5", "28,5"})
    void mapsPointsToRiskScore(int points, int expected) {
        assertThat(MifidQuestionnaire.riskScoreFor(points)).isEqualTo(expected);
    }

    @Test
    void rejectsUnknownAnswers() {
        assertThat(questionnaire.isValidAnswer("goal", "growth")).isTrue();
        assertThat(questionnaire.isValidAnswer("goal", "lottery")).isFalse();
        assertThat(questionnaire.isValidAnswer("favourite_colour", "blue")).isFalse();
    }
}
