package eval.model;

import java.util.List;

// ADK "Content" mirrors Gemini content: role + list of parts
public record Content(
        String role,           // "user" or "model"
        List<Part> parts
) {}
