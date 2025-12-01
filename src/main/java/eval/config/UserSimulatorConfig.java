package eval.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

// For user simulation configuration
public record UserSimulatorConfig(
        String model,                                   // "gemini-2.5-flash", etc.
        @JsonProperty("model_configuration")
        Map<String, Object> modelConfiguration,         // mirrors GenerateContentConfig
        @JsonProperty("max_allowed_invocations")
        Integer maxAllowedInvocations
) {}
