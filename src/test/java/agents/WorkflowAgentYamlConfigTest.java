package agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ConfigAgentUtils;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.SequentialAgent;
import config.RegistrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorkflowAgentYamlConfigTest {

    @BeforeAll
    public static void registerWrappersAndTools() {
        RegistrationService registrationService = new RegistrationService();
        registrationService.registerWorkflowWrappers();
        registrationService.registerFunctionTools();
    }

    @Test
    public void sequentialWorkflowAgent_canBeLoadedFromYaml() throws Exception {
        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/sequential-workflow-agent.yaml");

        assertNotNull(rootAgent);
        assertInstanceOf(SequentialAgent.class, rootAgent);

        List<? extends BaseAgent> subAgents = rootAgent.subAgents();
        assertEquals(1, subAgents.size());
        for (BaseAgent sub : subAgents) {
            assertInstanceOf(LlmAgent.class, sub);
        }
    }

    @Test
    public void parallelWorkflowAgent_canBeLoadedFromYaml() throws Exception {
        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/parallel-workflow-agent.yaml");

        assertNotNull(rootAgent);
        assertInstanceOf(ParallelAgent.class, rootAgent);

        List<? extends BaseAgent> subAgents = rootAgent.subAgents();
        assertEquals(1, subAgents.size());
        assertInstanceOf(LlmAgent.class, subAgents.get(0));
    }

    @Test
    public void loopWorkflowAgent_canBeLoadedFromYaml_withMaxIterations() throws Exception {
        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/loop-workflow-agent.yaml");

        assertNotNull(rootAgent);

        assertInstanceOf(LoopAgent.class, rootAgent);
        assertEquals("LoopWorkflowTest", rootAgent.name());

        List<? extends BaseAgent> subAgents = rootAgent.subAgents();
        assertEquals(1, subAgents.size());
        assertInstanceOf(LlmAgent.class, subAgents.get(0));
    }
}
