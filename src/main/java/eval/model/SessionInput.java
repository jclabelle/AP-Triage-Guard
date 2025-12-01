package eval.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// Initial session input (app name, user id, optional state)
public record SessionInput(
        @JsonProperty("app_name") String appName,
        @JsonProperty("user_id") String userId,
        Map<String, Object> state
) {}