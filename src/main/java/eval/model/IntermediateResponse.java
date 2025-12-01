package eval.model;
import java.util.List;

public record IntermediateResponse(
        String agentName,
        List<Part> parts
) {}
