package agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.adk.agents.*;
import config.WorkflowCallbackConfigHelper;

import java.util.List;

public abstract class LoopWorkflowAgent extends BaseAgent {
    private LoopWorkflowAgent() {
        super("LoopWorkflowAgent-placeholder", "Factory-only", List.of(),null, null);
    }

    public static BaseAgent fromConfig(BaseAgentConfig config, String configPath){

        if(config.name() == null || config.name().trim().isEmpty()) {
            throw new IllegalStateException(
                    "Missing configuration property: name in " + configPath
            );
        }

        try {
            var subAgents = ConfigAgentUtils.resolveSubAgents(config.subAgents(), configPath);

            Integer maxIterations = loadMaxIterations(configPath); // may return null

            var builder = LoopAgent.builder()
                    .name(config.name())
                    .description(config.description())
                    .subAgents(subAgents);

            if (maxIterations != null) {
                builder.maxIterations(maxIterations);
            }

            var helper = new WorkflowCallbackConfigHelper(configPath);

            for (Callbacks.BeforeAgentCallback cb : helper.beforeAgentCallbacks()) {
                builder.beforeAgentCallback(cb);
            }

            for (Callbacks.AfterAgentCallback cb : helper.afterAgentCallbacks()) {
                builder.afterAgentCallback(cb);
            }

            return builder.build();

        } catch  (ConfigAgentUtils.ConfigurationException e) {
            // Rethrow as unchecked exception. ConfigAgentUtils.fromConfig will catch and wrap into ConfigurationException
            throw new IllegalStateException("Failed to build LoopWorkflowAgent from " + configPath, e);
        }
    }

    // Never called.
    @Override
    protected io.reactivex.rxjava3.core.Flowable<com.google.adk.events.Event> runAsyncImpl(
            com.google.adk.agents.InvocationContext ctx) {
        throw new UnsupportedOperationException("LoopWorkflowAgent is factory-only.");
    }

    // Never called.
    @Override
    protected io.reactivex.rxjava3.core.Flowable<com.google.adk.events.Event> runLiveImpl(
            com.google.adk.agents.InvocationContext ctx) {
        throw new UnsupportedOperationException("LoopWorkflowAgent is factory-only.");
    }

    private static Integer loadMaxIterations(String configPath){

        try {
            var yamlText = java.nio.file.Files.readString(java.nio.file.Path.of(configPath));
            var mapper = new ObjectMapper(new YAMLFactory());
            var root = mapper.readValue(yamlText, java.util.Map.class);

            if (root == null) {
                return null;
            }

            Object raw = root.get("maxIterations");
            if (raw == null) {
                // Field not present
                return null;
            }

            // Accept numeric literals or numeric strings
            if (raw instanceof Number n) {
                int v =  n.intValue();
                if(v <= 0) {
                    throw new IllegalStateException("maxIterations must be > 0 in " + configPath);
                }
                return v;
            }
            if (raw instanceof String s) {
                s = s.trim();
                if (s.isEmpty()) {
                    return null; // treat empty as absent
                }
                int v =  Integer.parseInt(s);
                if(v <= 0) {
                    throw new IllegalStateException("maxIterations must be > 0 in " + configPath);
                }
                return v;
            }
            throw new IllegalStateException("maxIterations must be an integer (number or numeric string) in " + configPath);
        } catch (java.nio.file.NoSuchFileException e) {
            // Should not happen if ConfigAgentUtils already opened it, but treat as "no override"
            return null;
        } catch (java.io.IOException e) {
            // YAML unreadable
            throw new IllegalStateException(e);
        }



    }

}
