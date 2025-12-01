package userinterface;

import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import constants.FinalResponseAgentName;
import helpers.EventFunctionsHelper;
import io.reactivex.rxjava3.core.Flowable;
import observability.MetricsHelper;
import observability.RunRecorder;

import java.util.Objects;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChatUI {

    private final RunRecorder runRecorder = new RunRecorder();

    // Optional: allows passing the logical agent/app name,
    // so it shows up as a metric attribute.
    private final String agentName;

    public ChatUI() {
        this("HelpfulAgent"); // default
    }

    public ChatUI(String agentName) {
        this.agentName = agentName;
    }

    public void chat(Runner runner, Session session)
    {
        try (Scanner scanner = new Scanner(System.in, UTF_8))
        {
            while (true)
            {
                System.out.print("\nYou >");
                String userInput = scanner.nextLine();
                if ("quit".equalsIgnoreCase(userInput))
                {
                    break;
                }

                Content userMsg = Content.fromParts(Part.fromText(userInput));

                // ---- METRICS: start timing this "agent run" ----
                long startNanos = System.nanoTime();
                String status = "ok";

                try{
                    Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg);

                    System.out.print("\nAgent >");
                    events.blockingForEach(event -> {
                        if (event.finalResponse() && Objects.equals(event.author(), FinalResponseAgentName.VALUE)) {
                            System.out.println(event.stringifyContent());
                        }
                    });
                } catch (Exception e) {
                    status = "error";
                    System.err.println("Agent error: " + e.getMessage());
                } finally {
                    long durationMs = MetricsHelper.computeLatencyMs(startNanos);

                    // record the run
                    runRecorder.recordAgentRun(agentName, durationMs, status);

                }

            }
        }
    }
}
