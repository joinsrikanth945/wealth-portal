package com.yourbank.wealth.mifid;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The suitability questionnaire (version 2026.1) and its scoring.
 *
 * <p>Objectives, financial situation and attitude-to-risk answers are scored
 * 1-4 points each; the total (7-28) gives the risk profile 1-5. The knowledge
 * section decides whether complex products (derivatives) are appropriate.
 * In phase 8 the admin questionnaire editor will store versions in the
 * database; until then the definition lives here.
 */
@Component
public class MifidQuestionnaire {

    public static final String VERSION = "2026.1";

    public record Option(String id, String label, int points) {
    }

    public record Question(String id, String text, String help, boolean scored, List<Option> options) {

        Optional<Option> option(String optionId) {
            return options.stream().filter(o -> o.id().equals(optionId)).findFirst();
        }
    }

    public record Section(String id, String title, List<Question> questions) {
    }

    public record Definition(String version, List<Section> sections) {
    }

    public static final List<String> RISK_LABELS = List.of("Conservative", "Cautious", "Balanced", "Growth", "Dynamic");

    private static final Definition DEFINITION = new Definition(VERSION, List.of(
            new Section("objectives", "Objectives and horizon", List.of(
                    q("goal", "What is the main goal for this investment?", null, true,
                            o("preserve", "Preserve my capital", 1),
                            o("income", "Regular income with some growth", 2),
                            o("growth", "Long-term capital growth", 3),
                            o("speculation", "High returns, accepting large swings", 4)),
                    q("horizon", "How long do you plan to keep this money invested?", null, true,
                            o("lt2", "Less than 2 years", 1),
                            o("y2to5", "2 to 5 years", 2),
                            o("y5to10", "5 to 10 years", 3),
                            o("gt10", "More than 10 years", 4)))),
            new Section("financial", "Financial situation", List.of(
                    q("income", "What is your annual net income?", "All sources, after tax, in euros.", true,
                            o("lt50", "Under 50,000", 1),
                            o("50to100", "50,000 to 100,000", 2),
                            o("100to250", "100,000 to 250,000", 3),
                            o("gt250", "Over 250,000", 4)),
                    q("share", "What share of your total wealth does this investment represent?", null, true,
                            o("gt75", "More than 75%", 1),
                            o("50to75", "50% to 75%", 2),
                            o("25to50", "25% to 50%", 3),
                            o("lt25", "Less than 25%", 4)),
                    q("reserve", "How much emergency savings do you hold outside this investment?", null, true,
                            o("none", "None", 1),
                            o("lt3m", "Less than 3 months of expenses", 2),
                            o("3to6m", "3 to 6 months of expenses", 3),
                            o("gt6m", "More than 6 months of expenses", 4)))),
            new Section("risk", "Attitude to risk", List.of(
                    q("drawdown", "If your portfolio fell 20% in a year, what would you do?", null, true,
                            o("sell", "Sell everything", 1),
                            o("sellpart", "Sell part of it", 2),
                            o("hold", "Hold and wait for a recovery", 3),
                            o("buymore", "Invest more", 4)),
                    q("maxloss", "What is the largest loss in one year you could accept?", null, true,
                            o("l5", "Up to 5%", 1),
                            o("l10", "Up to 10%", 2),
                            o("l20", "Up to 20%", 3),
                            o("l30", "More than 20%", 4)))),
            new Section("knowledge", "Knowledge and experience", List.of(
                    q("exp_equities", "How often have you invested in shares or equity funds?", null, false,
                            o("none", "Never", 0),
                            o("some", "Occasionally", 1),
                            o("regular", "Regularly, for several years", 2)),
                    q("exp_derivatives", "How often have you traded options or futures?", null, false,
                            o("none", "Never", 0),
                            o("some", "A few times", 1),
                            o("regular", "Regularly", 2)),
                    q("leverage_quiz", "True or false: the buyer of a call option can lose more than the premium paid.",
                            "This checks understanding of derivatives risk.", false,
                            o("true", "True", 0),
                            o("false", "False", 1),
                            o("unsure", "I'm not sure", 0))))));

    public Definition definition() {
        return DEFINITION;
    }

    public List<Question> allQuestions() {
        return DEFINITION.sections().stream().flatMap(s -> s.questions().stream()).toList();
    }

    public Optional<Question> question(String questionId) {
        return allQuestions().stream().filter(q -> q.id().equals(questionId)).findFirst();
    }

    public boolean isValidAnswer(String questionId, String optionId) {
        return question(questionId).flatMap(q -> q.option(optionId)).isPresent();
    }

    /** Scores a (possibly partial) set of answers. */
    public Score score(Map<String, String> answers) {
        List<Question> questions = allQuestions();
        int answered = (int) questions.stream().filter(q -> answers.containsKey(q.id())).count();
        List<String> missing = questions.stream().map(Question::id).filter(id -> !answers.containsKey(id)).toList();

        int points = 0;
        boolean allScoredAnswered = true;
        for (Question q : questions) {
            if (!q.scored()) {
                continue;
            }
            Optional<Option> chosen = Optional.ofNullable(answers.get(q.id())).flatMap(q::option);
            if (chosen.isPresent()) {
                points += chosen.get().points();
            } else {
                allScoredAnswered = false;
            }
        }

        Integer riskScore = allScoredAnswered ? riskScoreFor(points) : null;
        boolean derivativesExperience = List.of("some", "regular").contains(answers.get("exp_derivatives"));
        boolean understandsLeverage = "false".equals(answers.get("leverage_quiz"));

        return new Score(
                points,
                riskScore,
                riskScore == null ? null : RISK_LABELS.get(riskScore - 1),
                derivativesExperience && understandsLeverage,
                answered,
                questions.size(),
                questions.isEmpty() ? 0 : Math.round(answered * 100f / questions.size()),
                missing);
    }

    /** 7-11 -> 1, 12-15 -> 2, 16-19 -> 3, 20-23 -> 4, 24-28 -> 5. */
    static int riskScoreFor(int points) {
        if (points <= 11) {
            return 1;
        }
        if (points <= 15) {
            return 2;
        }
        if (points <= 19) {
            return 3;
        }
        if (points <= 23) {
            return 4;
        }
        return 5;
    }

    public record Score(
            int points,
            Integer riskScore,
            String riskLabel,
            boolean complexProductsOk,
            int answered,
            int totalQuestions,
            int completionPercent,
            List<String> missingQuestions) {

        public boolean complete() {
            return missingQuestions.isEmpty();
        }
    }

    private static Question q(String id, String text, String help, boolean scored, Option... options) {
        return new Question(id, text, help, scored, List.of(options));
    }

    private static Option o(String id, String label, int points) {
        return new Option(id, label, points);
    }
}
