package eval.metrics;

import eval.config.CriterionConfig;
import eval.model.Content;
import eval.model.EvalCase;
import eval.model.Invocation;
import eval.model.Part;

import java.util.*;
import java.util.stream.Collectors;

public final class ResponseMatchScoreCriterion implements EvaluationCriterion {

    public static final String NAME = "response_match_score";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public double evaluate(EvalCase expected,
                           List<Invocation> actualConversation,
                           CriterionConfig config) {

        List<Invocation> expectedInvocations = expected.conversation();
        if (expectedInvocations.isEmpty()) {
            return 1.0;
        }

        double total = 0.0;
        int count = Math.min(expectedInvocations.size(), actualConversation.size());

        for (int i = 0; i < count; i++) {
            String expectedText = joinParts(expectedInvocations.get(i).finalResponse());
            String actualText = joinParts(actualConversation.get(i).finalResponse());
            total += rouge1F1(expectedText, actualText);
        }

        return total / count;
    }

    private String joinParts(Content content) {
        if (content == null || content.parts() == null) {
            return "";
        }
        return content.parts().stream()
                .map(Part::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private double rouge1F1(String expected, String actual) {
        List<String> refTokens = tokenize(expected);
        List<String> hypTokens = tokenize(actual);

        if (refTokens.isEmpty() || hypTokens.isEmpty()) {
            return 0.0;
        }

        Map<String, Integer> refCounts = countTokens(refTokens);
        Map<String, Integer> hypCounts = countTokens(hypTokens);

        int overlap = 0;
        for (Map.Entry<String, Integer> e : refCounts.entrySet()) {
            int inHyp = hypCounts.getOrDefault(e.getKey(), 0);
            overlap += Math.min(e.getValue(), inHyp);
        }

        double precision = (double) overlap / hypTokens.size();
        double recall = (double) overlap / refTokens.size();
        if (precision == 0.0 && recall == 0.0) {
            return 0.0;
        }
        return 2 * precision * recall / (precision + recall);
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase().split("\\s+"))
                .map(t -> t.replaceAll("\\p{Punct}+", ""))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private Map<String, Integer> countTokens(List<String> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tokens) {
            counts.merge(t, 1, Integer::sum);
        }
        return counts;
    }
}
