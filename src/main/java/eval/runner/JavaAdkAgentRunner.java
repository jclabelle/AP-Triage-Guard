package eval.runner;

import eval.model.*;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import io.reactivex.rxjava3.core.Flowable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// Bridge between the project's EvalSet DTOs and the Java ADK runtime
public final class JavaAdkAgentRunner implements AgentRunner {

    private final InMemoryRunner runner;
    private final RunConfig runConfig;

    private static final Logger LOG = LoggerFactory.getLogger(JavaAdkAgentRunner.class);

    public JavaAdkAgentRunner(BaseAgent rootAgent) {
        this.runner = new InMemoryRunner(rootAgent);
        this.runConfig = RunConfig.builder().build();
    }

    @Override
    public List<Invocation> runCase(EvalCase evalCase) {

        SessionInput si = evalCase.sessionInput();

        // 1x session per eval case
        Session session = runner.sessionService()
                .createSession(si.appName(), si.userId())
                .blockingGet();

        List<Invocation> actualInvocations = new ArrayList<>();

        // 1x ADK run per user turn
        for(Invocation expected : evalCase.conversation()){
            com.google.genai.types.Content userMsg = toGenAiContent(expected.userContent());

            Flowable<Event> events =
                    runner.runAsync(session.userId(), session.id(), userMsg, runConfig);

            Invocation actual = collectInvocationFromEvents(expected, events);
            actualInvocations.add(actual);

            int toolCount = actual.intermediateData() != null
                    ? actual.intermediateData().toolUses().size()
                    : 0;
            String finalText = (actual.finalResponse() != null
                    && actual.finalResponse().parts() != null
                    && !actual.finalResponse().parts().isEmpty())
                    ? actual.finalResponse().parts().get(0).text()
                    : "";

            String finalPreview = finalText == null
                    ? ""
                    : (finalText.length() > 120 ? finalText.substring(0, 120) + "..." : finalText);

            LOG.info(
                    "Collected invocation '{}' for eval case '{}' with {} tool calls and final text preview: {}",
                    actual.invocationId(),
                    evalCase.evalId(),
                    toolCount,
                    finalPreview
            );
        }

        return actualInvocations;
    }

    // Maps our local eval Content DTOs to com.google.genai.types.Content for ADK
    private com.google.genai.types.Content toGenAiContent(eval.model.Content evalContent) {
        List<com.google.genai.types.Part> parts =
                evalContent.parts().stream()
                        .map(p -> com.google.genai.types.Part.fromText(p.text()))
                        .toList();

        return com.google.genai.types.Content
                .builder()
                .role(evalContent.role())
                .parts(parts)
                .build();
    }

    // Walk the event stream for a single turn, looking for ToolUse, final_response and intermediate_data.
    private Invocation collectInvocationFromEvents(Invocation expected, Flowable<Event> eventStream)
    {
        List<ToolUse> toolUses = new ArrayList<>();
        List<IntermediateResponse> intermediateResponses = new ArrayList<>();

        // Accumulate partial chunks of streaming text, then flush when we hit the final_response
        StringBuilder streamingBuffer = new StringBuilder();
        AtomicReference<String> finalTextRef = new AtomicReference<>("");

        eventStream.blockingForEach(event ->
        {
            // Tool Calls -> ToolUse[]
            List<FunctionCall> calls = event.functionCalls();

            if (!calls.isEmpty()) {
                for (FunctionCall call : calls) {
                    String toolName = call.name().orElse("unknown_tool");
                    Map<String, Object> args =
                            call.args().map(Map::copyOf).orElse(Map.of());

                    // Use Event id as stable id for the tool use
                    toolUses.add(
                            new ToolUse(
                                    event.id(),
                                    toolName,
                                    args
                            )
                    );
                }
            }

            // Accumulate streaming text
            if (event.partial().orElse(false) && event.content().isPresent()) {
                event.content().ifPresent(genContent ->
                {
                    var partsOpt = genContent.parts();
                    if (partsOpt.isEmpty() || partsOpt.get().isEmpty()) {
                        return;
                    }

                    var firstPart = partsOpt.get().get(0);
                    String text = firstPart.text().orElse("");
                    if (!text.isEmpty()) {
                        streamingBuffer.append(text);
                    }
                });
            }

            // Final vs Intermediate responses
            if (event.finalResponse()) {
                if (event.content().isPresent()) {
                    event.content().ifPresent(genContent ->
                    {
                        var partsOpt = genContent.parts();
                        if (partsOpt.isPresent()
                                && !partsOpt.get().isEmpty()
                                && partsOpt.get().get(0).text().isPresent()) {
                            String eventText = partsOpt.get().get(0).text().get();
                            String finalText =
                                    streamingBuffer.toString()
                                            + (event.partial().orElse(false) ? "" : eventText);

                            finalTextRef.set(finalText.trim());
                            streamingBuffer.setLength(0);
                            return;
                        }
                    });
                }

                // Fallback: final due to skip_summarization + functionResponses (raw tool result)
                if (event.actions() != null
                        && event.actions().skipSummarization().orElse(false)
                        && !event.functionResponses().isEmpty()) {
                    FunctionResponse resp = event.functionResponses().get(0);
                    Map<String, Object> responseData = resp.response().map(Map::copyOf).orElse(Map.of());
                    finalTextRef.set(responseData.toString());
                    return;
                }

                // Fallback: Long-running tool
                if (event.longRunningToolIds().isPresent()
                        && !event.longRunningToolIds().get().isEmpty()) {
                    finalTextRef.set("Tool is running in the background.");
                    return;
                }
            } else {
                // Non‑final, non‑partial text => treat as intermediate response for observability.
                event.content().ifPresent(genContent ->
                {
                    var partsOpt = genContent.parts();
                    if (partsOpt.isEmpty() || partsOpt.get().isEmpty()) {
                        return;
                    }

                    var firstPart = partsOpt.get().get(0);
                    String text = firstPart.text().orElse("");
                    if (!text.isEmpty()
                            && !event.partial().orElse(false)) {
                        intermediateResponses.add(
                                new IntermediateResponse(
                                        event.author(), // Either Agent name or user
                                        List.of(new Part(text))
                                )
                        );
                    }
                });
            }
        });

        // Build eval-side final_response Content
        String finalText = finalTextRef.get();
        // If no explicit final response was captured but we have buffered text,
        // fall back to the buffered content so we don't silently drop model output.
        if ((finalText == null || finalText.isBlank()) && streamingBuffer.length() > 0) {
            finalText = streamingBuffer.toString().trim();
        }
        if (finalText == null) {
            finalText = "";
        }

        Content finalResponse =
                new Content(
                        "model",
                        List.of(new Part(finalText))
                );

        IntermediateData intermediateData =
                new IntermediateData(toolUses, intermediateResponses);

        // Keep the invocation_id from the expected case to ease correlation
        Invocation invocation = new Invocation(
                expected.invocationId(),
                expected.userContent(),
                finalResponse,
                intermediateData
        );

        return invocation;
    }
}
