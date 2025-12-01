package eval.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record JudgeModelOptions(
        @JsonProperty("judge_model") String judgeModel, // e.g. "gemini-2.5-flash"
        @JsonProperty("num_samples") Integer numSamples,
        @JsonProperty("model_type") String modelType,   // optional, for custom providers
        @JsonProperty("model_config") Map<String, Object> modelConfig
) {}
