package eval.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Expected tools + intermediate responses between user prompt and final reply
public record IntermediateData(
        @JsonProperty("tool_uses") List<ToolUse> toolUses,
        @JsonProperty("intermediate_responses") List<IntermediateResponse> intermediateResponses
) {}
