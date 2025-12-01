package eval.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Top-level container for a .test.json or .evalset.json file
public record EvalSet(
        @JsonProperty("eval_set_id") String evalSetId,
        String name,
        String description,
        @JsonProperty("eval_cases") List<EvalCase> evalCases
) {}
