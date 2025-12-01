package eval.model;

import com.fasterxml.jackson.annotation.JsonProperty;

// One user -> agent interaction including expected behaviour
public record Invocation(
        @JsonProperty("invocation_id") String invocationId,
        @JsonProperty("user_content") Content userContent,
        @JsonProperty("final_response") Content finalResponse,
        @JsonProperty("intermediate_data") IntermediateData intermediateData
) {}
