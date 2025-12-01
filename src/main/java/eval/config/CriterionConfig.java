package eval.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Represents both the simple and detailed forms of a criterion entry:
// - 0.8                    -> threshold = 0.8
// - {"threshold": 0.8, ...} -> threshold + other fields
public record CriterionConfig(
        Double threshold,                               // 0.0–1.0
        @JsonProperty("match_type") String matchType,   // EXACT / IN_ORDER / ANY_ORDER (for tool_trajectory)
        @JsonProperty("evaluate_intermediate_nl_responses")
        Boolean evaluateIntermediateNlResponses,        // for hallucinations_v1
        List<Rubric> rubrics,                           // for rubric-based metrics
        @JsonProperty("judge_model_options")
        JudgeModelOptions judgeModelOptions             // for LLM-as-judge metrics
) {}
