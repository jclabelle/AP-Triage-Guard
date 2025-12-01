package eval.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record EvalConfig(
        Map<String, CriterionConfig> criteria,
        @JsonProperty("user_simulator_config") UserSimulatorConfig userSimulatorConfig
) {}
