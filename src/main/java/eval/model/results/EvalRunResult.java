package eval.model.results;

import java.util.List;

public record EvalRunResult(
        String evalSetId,
        List<EvalCaseResult> caseResults
) {
    public boolean passed() {
        return caseResults.stream().allMatch(EvalCaseResult::passed);
    }
}
