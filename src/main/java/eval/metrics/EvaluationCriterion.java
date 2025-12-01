package eval.metrics;

import eval.config.CriterionConfig;
import eval.model.EvalCase;
import eval.model.Invocation;

import java.util.List;

public interface EvaluationCriterion {

    /** e.g. "tool_trajectory_avg_score", "response_match_score" */
    String name();

    /**
     * Compute a score for this metric on one eval case.
     *
     * @param expected The expected eval case (from the EvalSet JSON).
     * @param actualConversation The actual conversation produced by the agent.
     * @param config The metric-specific config from EvalConfig.criteria().
     * @return Score in [0.0, 1.0].
     */
    double evaluate(EvalCase expected,
                    List<Invocation> actualConversation,
                    CriterionConfig config);
}

