package agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ConfigAgentUtils;
import com.google.adk.agents.InvocationContext;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.artifacts.BaseArtifactService;
import com.google.adk.artifacts.InMemoryArtifactService;
import com.google.adk.events.Event;
import com.google.adk.sessions.BaseSessionService;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.sessions.Session;
import com.google.genai.types.AudioTranscriptionConfig;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.SpeechConfig;
import io.reactivex.rxjava3.core.Flowable;
import config.RegistrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration-style test that exercises LoopAgent end-to-end with a simple
 * in-memory InvocationContext and a dummy sub-agent. It verifies that
 * maxIterations controls how many times the sub-agent is invoked.
 */
public class LoopAgentIterationIntegrationTest {

    @BeforeAll
    public static void registerWrappersAndTools() {
        RegistrationService registrationService = new RegistrationService();
        registrationService.registerWorkflowWrappers();
        registrationService.registerFunctionTools();
    }

    @Test
    public void loopAgent_loadedFromYaml_respectsMaxIterations_whenRunningSubAgent() throws Exception {
        DummyCountingAgent.resetCount();

        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/loop-workflow-agent.yaml");

        assertNotNull(rootAgent);
        assertInstanceOf(LoopAgent.class, rootAgent);

        LoopAgent loopAgent = (LoopAgent) rootAgent;

        // Replace YAML-configured sub-agents with a dummy counting agent so we
        // can observe how many times the loop executes without invoking LLMs.
        Field subAgentsField = BaseAgent.class.getDeclaredField("subAgents");
        subAgentsField.setAccessible(true);
        subAgentsField.set(loopAgent, List.of(new DummyCountingAgent()));

        // Minimal in-memory services for InvocationContext.
        BaseSessionService sessionService = new InMemorySessionService();
        BaseArtifactService artifactService = new InMemoryArtifactService();

        Session session = Session.builder("LoopIterationTestApp")
                .id("session-1")
                .userId("test-user")
                .state(new ConcurrentHashMap<>())
                .events(List.of())
                .build();

        RunConfig runConfig = RunConfig.builder()
                .setSpeechConfig(SpeechConfig.builder().languageCode("en-US").build())
                .setResponseModalities(List.of())
                .setSaveInputBlobsAsArtifacts(false)
                .setStreamingMode(RunConfig.StreamingMode.NONE)
                .setOutputAudioTranscription(AudioTranscriptionConfig.builder().build())
                .setMaxLlmCalls(100)
                .build();

        Content userContent = Content.fromParts(Part.fromText("loop iteration test"));

        InvocationContext ctx = InvocationContext.create(
                sessionService,
                artifactService,
                "LoopIterationFromYamlTestApp",
                loopAgent,
                session,
                userContent,
                runConfig
        );

        Flowable<Event> events = loopAgent.runAsync(ctx);
        List<Event> emittedEvents = events.toList().blockingGet();

        assertNotNull(emittedEvents);
        assertEquals(3, DummyCountingAgent.getInvocationCount());
    }
}
