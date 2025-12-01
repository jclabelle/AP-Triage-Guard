package eval.model.results;

import java.util.List;

public record EvalCaseResult(
        String evalId,
        List<MetricScore> metricScores
) {
    public boolean passed() {
        return metricScores.stream().allMatch(MetricScore::passed);
    }
}
