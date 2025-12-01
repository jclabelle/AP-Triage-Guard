package agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ConfigAgentUtils;
import com.google.adk.agents.LoopAgent;
import config.RegistrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test intentionally uses reflection to inspect the private maxIterations field
 * on LoopAgent to verify that our YAML configuration pipeline correctly applies
 * the configured maxIterations value. It is kept separate from the main workflow
 * tests because it is brittle against internal ADK implementation changes.
 */
public class LoopAgentMaxIterationsReflectionTest {

    @BeforeAll
    public static void registerWrappersAndTools() {
        RegistrationService registrationService = new RegistrationService();
        registrationService.registerWorkflowWrappers();
        registrationService.registerFunctionTools();
    }

    @Test
    public void loopWorkflowAgent_yaml_setsMaxIterationsField() throws Exception {
        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/loop-workflow-agent.yaml");

        assertNotNull(rootAgent);
        assertInstanceOf(LoopAgent.class, rootAgent);

        LoopAgent loopAgent = (LoopAgent) rootAgent;

        Field maxIterationsField = LoopAgent.class.getDeclaredField("maxIterations");
        maxIterationsField.setAccessible(true);

        Object rawValue = maxIterationsField.get(loopAgent);
        assertNotNull(rawValue);
        assertInstanceOf(Optional.class, rawValue);

        @SuppressWarnings("unchecked")
        Optional<Integer> maxIterations = (Optional<Integer>) rawValue;

        assertTrue(maxIterations.isPresent());
        assertEquals(3, maxIterations.get().intValue());
    }
}

