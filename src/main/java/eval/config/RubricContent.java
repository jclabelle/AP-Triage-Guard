package eval.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RubricContent(
        @JsonProperty("text_property") String textProperty
) {}
