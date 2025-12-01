package orchestrator;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ConfigAgentUtils;
import com.google.adk.memory.InMemoryMemoryService;
import com.google.adk.plugins.LoggingPlugin;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.sessions.Session;
import com.google.adk.tools.FunctionTool;
import com.google.adk.utils.ComponentRegistry;
import config.RegistrationService;
import tools.DayOfWeekService;
import userinterface.ChatUI;

import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class AppOrchestrator {

    public String appName = "APTriageGuard";
    public String userId = "APUser";

    public void runApp() throws ConfigAgentUtils.ConfigurationException {

        var sessionId = UUID.randomUUID().toString();

        InMemorySessionService sessionService = new InMemorySessionService();
        InMemoryMemoryService memoryService = new InMemoryMemoryService();
        ConcurrentHashMap<String, Object> initialState = new ConcurrentHashMap<>();

        Session apSession = sessionService.createSession(appName, userId, initialState, sessionId).blockingGet();

        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/main/resources/agents/invoice/ap-invoice-pipeline.yaml");

        Runner runner =
                new Runner(rootAgent, appName, null, sessionService, memoryService,
                        List.of(new LoggingPlugin()));

        ChatUI chatUI = new ChatUI(appName);
        chatUI.chat(runner, apSession);
    }


}
