package agents;

import com.google.adk.agents.*;
import config.WorkflowCallbackConfigHelper;

import java.util.List;

public abstract class SequentialWorkflowAgent extends BaseAgent {

    // Never instantiated. Constructor exists to satisfy BaseAgent.
    private SequentialWorkflowAgent() {
        super(
                "SequentialWorkflowAgent-placeholder",
                "Factory-only BaseAgent subclass",
                List.of(),
                null,
                null
        );
    }

    public static BaseAgent fromConfig(BaseAgentConfig config, String configPath)
    {

        if(config.name() == null || config.name().trim().isEmpty()) {
            throw new IllegalStateException(
                    "Missing configuration property: name in " + configPath
            );
        }

        try {
            // Resolve subagents from YAML `subAgents` entries relative to this file.
            var subAgents = ConfigAgentUtils.resolveSubAgents(
                    config.subAgents(), //List<BaseAgentConfig.AgentRefConfig>
                    configPath // path to this YAML file
            );


            var builder = SequentialAgent.builder()
                    .name(config.name())
                    .description(config.description())
                    .subAgents(subAgents);

            var helper = new WorkflowCallbackConfigHelper(configPath);

            for (Callbacks.BeforeAgentCallback cb : helper.beforeAgentCallbacks()) {
                builder.beforeAgentCallback(cb);
            }

            for (Callbacks.AfterAgentCallback cb : helper.afterAgentCallbacks()) {
                builder.afterAgentCallback(cb);
            }

            return builder.build();
        } catch (ConfigAgentUtils.ConfigurationException e) {
            // Rethrow as unchecked exception. ConfigAgentUtils.fromConfig will catch and wrap into ConfigurationException
            throw new IllegalStateException("Failed to build SequentialWorkflowAgent from " + configPath, e);
        }


    }

    // Never called.
    @Override
    protected io.reactivex.rxjava3.core.Flowable<com.google.adk.events.Event> runAsyncImpl(
            com.google.adk.agents.InvocationContext ctx) {
        throw new UnsupportedOperationException("SequentialWorkflowAgent is factory-only.");
    }

    // Never called.
    @Override
    protected io.reactivex.rxjava3.core.Flowable<com.google.adk.events.Event> runLiveImpl(
            com.google.adk.agents.InvocationContext ctx) {
        throw new UnsupportedOperationException("SequentialWorkflowAgent is factory-only.");
    }


}
