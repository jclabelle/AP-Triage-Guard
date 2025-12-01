package eval.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// One evaluation case = one session
public record EvalCase(
        @JsonProperty("eval_id") String evalId,
        @JsonProperty("session_input") SessionInput sessionInput,
        List<Invocation> conversation
) {}
