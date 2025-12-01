package eval.model;

import java.util.Map;

// Expected tool call (name + arguments, optional ADK-generated id)
public record ToolUse(
        String id,             // may be null / omitted in JSON
        String name,
        Map<String, Object> args
) {}