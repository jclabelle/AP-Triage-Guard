package eval.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Rubric(
        @JsonProperty("rubric_id") String rubricId,
        @JsonProperty("rubric_content") RubricContent rubricContent
) {}
