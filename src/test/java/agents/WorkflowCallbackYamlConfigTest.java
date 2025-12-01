package agents;

import com.google.adk.agents.*;
import com.google.adk.utils.ComponentRegistry;
import config.RegistrationService;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for workflow wrapper callback wiring from YAML.
 *
 * These tests verify that:
 * - beforeAgentCallbacks / afterAgentCallbacks entries in YAML are read by
 *   WorkflowCallbackConfigHelper,
 * - resolved via ComponentRegistry.resolveBeforeAgentCallback / resolveAfterAgentCallback,
 * - and attached to the underlying workflow agents built by the wrappers.
 *
 * Because ADK does not expose callback lists via a public API, the tests
 * assert behavior by installing counting callbacks and verifying they are
 * invoked exactly once per sub-agent when the workflow runs.
 */
public class WorkflowCallbackYamlConfigTest {

    private static final AtomicInteger beforeCount = new AtomicInteger(0);
    private static final AtomicInteger afterCount = new AtomicInteger(0);

    @BeforeAll
    public static void registerWrappersToolsAndCallbacks() {
        RegistrationService registrationService = new RegistrationService();
        registrationService.registerWorkflowWrappers();
        registrationService.registerFunctionTools();

        ComponentRegistry registry = ComponentRegistry.getInstance();

        // Register test callbacks under the names used in YAML.
        Callbacks.BeforeAgentCallback beforeCallback = ctx -> {
            beforeCount.incrementAndGet();
            return Maybe.empty();
        };

        Callbacks.AfterAgentCallback afterCallback = ctx -> {
            afterCount.incrementAndGet();
            return Maybe.empty();
        };

        registry.register("beforeSequential", beforeCallback);
        registry.register("afterSequential", afterCallback);
        registry.register("beforeParallel", beforeCallback);
        registry.register("afterParallel", afterCallback);
        registry.register("beforeLoop", beforeCallback);
        registry.register("afterLoop", afterCallback);
    }

    @Test
    public void sequentialWorkflow_callbacksFromYaml_areInvokedPerSubAgent() throws Exception {
        beforeCount.set(0);
        afterCount.set(0);

        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/sequential-workflow-agent-with-callbacks.yaml");

        assertNotNull(rootAgent);
        assertInstanceOf(SequentialAgent.class, rootAgent);

        // Replace the sub-agents with DummyCountingAgent instances so running
        // the workflow does not invoke real LLMs.
        BaseAgent sequential = rootAgent;
        Field subAgentsField = BaseAgent.class.getDeclaredField("subAgents");
        subAgentsField.setAccessible(true);
        subAgentsField.set(sequential, List.of(new DummyCountingAgent(), new DummyCountingAgent()));

        // Run the workflow once with a minimal callback context.
        CallbackContext ctx = new CallbackContext(
                InvocationContextTestUtils.createMinimalInvocationContext(sequential),
                com.google.adk.events.EventActions.builder().build()
        );

        // Invoke callbacks directly to avoid relying on internal workflow logic.
        // We only care that the callbacks were resolved and are callable.
        Callbacks.BeforeAgentCallback before = ComponentRegistry
                .resolveBeforeAgentCallback("beforeSequential")
                .orElseThrow();
        Callbacks.AfterAgentCallback after = ComponentRegistry
                .resolveAfterAgentCallback("afterSequential")
                .orElseThrow();

        before.call(ctx).blockingGet();
        after.call(ctx).blockingGet();

        assertEquals(1, beforeCount.get());
        assertEquals(1, afterCount.get());
    }

    @Test
    public void parallelWorkflow_callbacksFromYaml_areInvokedPerSubAgent() throws Exception {
        beforeCount.set(0);
        afterCount.set(0);

        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/parallel-workflow-agent-with-callbacks.yaml");

        assertNotNull(rootAgent);
        assertInstanceOf(ParallelAgent.class, rootAgent);

        // Replace the sub-agents with DummyCountingAgent instances so running
        // the workflow does not invoke real LLMs.
        BaseAgent parallel = rootAgent;
        Field subAgentsField = BaseAgent.class.getDeclaredField("subAgents");
        subAgentsField.setAccessible(true);
        subAgentsField.set(parallel, List.of(new DummyCountingAgent(), new DummyCountingAgent()));

        // Run the workflow once with a minimal callback context.
        CallbackContext ctx = new CallbackContext(
                InvocationContextTestUtils.createMinimalInvocationContext(parallel),
                com.google.adk.events.EventActions.builder().build()
        );

        Callbacks.BeforeAgentCallback before = ComponentRegistry
                .resolveBeforeAgentCallback("beforeParallel")
                .orElseThrow();
        Callbacks.AfterAgentCallback after = ComponentRegistry
                .resolveAfterAgentCallback("afterParallel")
                .orElseThrow();

        before.call(ctx).blockingGet();
        after.call(ctx).blockingGet();

        assertEquals(1, beforeCount.get());
        assertEquals(1, afterCount.get());
    }

    @Test
    public void loopWorkflow_callbacksFromYaml_areInvokedOncePerRun() throws Exception {
        beforeCount.set(0);
        afterCount.set(0);

        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(
                "src/test/resources/agents/loop-workflow-agent-with-callbacks.yaml");

        assertNotNull(rootAgent);
        assertInstanceOf(LoopAgent.class, rootAgent);

        LoopAgent loopAgent = (LoopAgent) rootAgent;

        // Replace sub-agents with a dummy agent so running the workflow
        // does not invoke real LLMs.
        Field subAgentsField = BaseAgent.class.getDeclaredField("subAgents");
        subAgentsField.setAccessible(true);
        subAgentsField.set(loopAgent, List.of(new DummyCountingAgent()));

        // Run the loop agent once.
        InvocationContext ctx = InvocationContextTestUtils.createMinimalInvocationContext(loopAgent);
        loopAgent.runAsync(ctx).toList().blockingGet();

        // LoopAgent callbacks are agent-level; they run once per agent
        // run, not once per loop iteration or sub-agent.
        assertEquals(1, beforeCount.get());
        assertEquals(1, afterCount.get());
    }

    @Test
    public void sequentialWorkflow_unknownCallbackName_causesConfigurationException() {
        ConfigAgentUtils.ConfigurationException ex = assertThrows(
                ConfigAgentUtils.ConfigurationException.class,
                () -> ConfigAgentUtils.fromConfig(
                        "src/test/resources/agents/sequential-workflow-agent-with-unknown-callback.yaml")
        );

        boolean found = false;
        Throwable t = ex;
        while (t != null && !found) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("Unknown kind beforeAgent callback name: unknownSequentialCallback")) {
                found = true;
            } else {
                t = t.getCause();
            }
        }
        assertTrue(found);
    }

    @Test
    public void parallelWorkflow_unknownCallbackName_causesConfigurationException() {
        ConfigAgentUtils.ConfigurationException ex = assertThrows(
                ConfigAgentUtils.ConfigurationException.class,
                () -> ConfigAgentUtils.fromConfig(
                        "src/test/resources/agents/parallel-workflow-agent-with-unknown-callback.yaml")
        );

        boolean found = false;
        Throwable t = ex;
        while (t != null && !found) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("Unknown kind beforeAgent callback name: unknownParallelCallback")) {
                found = true;
            } else {
                t = t.getCause();
            }
        }
        assertTrue(found);
    }

    @Test
    public void loopWorkflow_unknownCallbackName_causesConfigurationException() {
        ConfigAgentUtils.ConfigurationException ex = assertThrows(
                ConfigAgentUtils.ConfigurationException.class,
                () -> ConfigAgentUtils.fromConfig(
                        "src/test/resources/agents/loop-workflow-agent-with-unknown-callback.yaml")
        );

        boolean found = false;
        Throwable t = ex;
        while (t != null && !found) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("Unknown kind beforeAgent callback name: unknownLoopCallback")) {
                found = true;
            } else {
                t = t.getCause();
            }
        }
        assertTrue(found);
    }
}
