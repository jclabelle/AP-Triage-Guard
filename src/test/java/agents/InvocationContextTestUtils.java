package agents;

import com.google.adk.agents.InvocationContext;
import com.google.adk.agents.RunConfig;
import com.google.adk.artifacts.BaseArtifactService;
import com.google.adk.artifacts.InMemoryArtifactService;
import com.google.adk.memory.BaseMemoryService;
import com.google.adk.memory.InMemoryMemoryService;
import com.google.adk.plugins.PluginManager;
import com.google.adk.sessions.BaseSessionService;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.sessions.Session;
import com.google.genai.types.AudioTranscriptionConfig;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.SpeechConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small helper for constructing minimal InvocationContext instances for tests.
 */
final class InvocationContextTestUtils {

    private InvocationContextTestUtils() {
    }

    static InvocationContext createMinimalInvocationContext(com.google.adk.agents.BaseAgent agent) {
        BaseSessionService sessionService = new InMemorySessionService();
        BaseArtifactService artifactService = new InMemoryArtifactService();
        BaseMemoryService memoryService = new InMemoryMemoryService();
        PluginManager pluginManager = new PluginManager();

        Session session = Session.builder("TestApp")
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

        Content userContent = Content.fromParts(Part.fromText("callback test"));

        return InvocationContext.create(
                sessionService,
                artifactService,
                "TestApp",
                agent,
                session,
                userContent,
                runConfig
        );
    }
}
