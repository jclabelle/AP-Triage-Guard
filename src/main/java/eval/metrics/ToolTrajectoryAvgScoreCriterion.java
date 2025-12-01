package eval.metrics;

import eval.config.CriterionConfig;
import eval.model.EvalCase;
import eval.model.Invocation;
import eval.model.ToolUse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ToolTrajectoryAvgScoreCriterion implements EvaluationCriterion {

    public static final String NAME = "tool_trajectory_avg_score";

    @Override
    public String name(){
        return NAME;
    }

    @Override
    public double evaluate(EvalCase expected,
                           List<Invocation> actualConversation,
                           CriterionConfig config) {

        String matchType = config.matchType() != null
                ? config.matchType()
                : "EXACT";

        List<Invocation> expectedInvocations = expected.conversation();

        if (expectedInvocations.isEmpty()) {
            return 1.0; // nothing to compare
        }

        double total = 0.0;
        int count = Math.min(expectedInvocations.size(), actualConversation.size());

        for (int i = 0; i < count; i++) {
            List<ToolUse> expectedTools = expectedInvocations.get(i)
                    .intermediateData().toolUses();
            List<ToolUse> actualTools = actualConversation.get(i)
                    .intermediateData().toolUses();

            boolean match = switch (matchType) {
                case "EXACT" -> exactMatch(expectedTools, actualTools);
                case "IN_ORDER" -> inOrderMatch(expectedTools, actualTools);
                case "ANY_ORDER" -> anyOrderMatch(expectedTools, actualTools);
                default -> exactMatch(expectedTools, actualTools);
            };

            total += match ? 1.0 : 0.0;
        }
        return total / count;
    }

    private boolean exactMatch(List<ToolUse> expectedTools, List<ToolUse> actualTools) {
        if(expectedTools.size() != actualTools.size()) {
            return false;
        }

        for (int i = 0; i < expectedTools.size(); i++)
        {
            if(!toolEquals(expectedTools.get(i), actualTools.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    private boolean inOrderMatch(List<ToolUse> expectedTools, List<ToolUse> actualTools) {
        if(expectedTools.isEmpty()) {
            return true;
        }

        int j = 0;
        for(ToolUse a: actualTools) {
            if(toolEquals(expectedTools.get(j), a)) {
                j++;
                if(j == expectedTools.size()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anyOrderMatch(List<ToolUse> expectedTools, List<ToolUse> actualTools) {
        if(expectedTools.isEmpty()) {
            return true;
        }

        List<ToolUse> remaining = new ArrayList<>(actualTools);
        for(ToolUse e:  expectedTools) {
            int idx = indexOfTool(remaining, e);
            if(idx < 0){
                return false;
            }
            remaining.remove(idx);
        }
        return true;
    }

    private int indexOfTool(List<ToolUse> list, ToolUse target){
        for(int i = 0; i < list.size(); i++){
            if(toolEquals(list.get(i), target)){
                return i;
            }
        }
        return -1;
    }

    private boolean toolEquals(ToolUse a,  ToolUse b) {
        if(!Objects.equals(a.name(), b.name())) {
            return false;
        }

        // todo: compare this to Python version
        // Fallback to Map equality for args
        return Objects.equals(a.args(), b.args());
    }
}
