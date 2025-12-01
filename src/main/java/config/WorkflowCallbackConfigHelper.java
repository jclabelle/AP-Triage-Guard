package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.adk.agents.Callbacks;
import com.google.adk.agents.ConfigAgentUtils;
import com.google.adk.utils.ComponentRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class WorkflowCallbackConfigHelper {

    private final Map<String, Object> root;

    public WorkflowCallbackConfigHelper(String configPath) {
        this.root = loadRoot(configPath);
    }

    public List<Callbacks.BeforeAgentCallback> beforeAgentCallbacks()
            throws ConfigAgentUtils.ConfigurationException {
        return resolveCallbacks(
                "beforeAgentCallbacks",
                ComponentRegistry::resolveBeforeAgentCallback,
                "beforeAgent"
        );
    }

    public List<Callbacks.AfterAgentCallback> afterAgentCallbacks()
            throws ConfigAgentUtils.ConfigurationException {
        return resolveCallbacks(
                "afterAgentCallbacks",
                ComponentRegistry::resolveAfterAgentCallback,
                "afterAgent"
        );
    }

    private static Map<String, Object> loadRoot(String configPath) {
        try {
            String yaml = Files.readString(Path.of(configPath));
            var mapper = new ObjectMapper(new YAMLFactory());
            Object obj = mapper.readValue(yaml, Map.class);
            return (obj instanceof Map) ? (Map<String, Object>) obj : Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load workflow callback config in" + configPath, e);
        }
    }

    private <T> List<T> resolveCallbacks(
            String yamlKey,
            java.util.function.Function<String, Optional<T>> resolver,
            String kind
    ) throws ConfigAgentUtils.ConfigurationException {

        Object raw = root.get(yamlKey);
        if(raw == null) {
            return List.of();
        }
        if(!(raw instanceof List<?> list)) {
            throw new IllegalStateException(yamlKey + " must be a list in workflow YAML");
        }

        List<T> result = new ArrayList<>();
        for (Object elem : list){

            if(!(elem instanceof Map<?, ?> map)) {
                throw new IllegalStateException("Each " + yamlKey + " entry must be a Map with 'name'");
            }

            Object nameObj = map.get("name");
            if(!(nameObj instanceof String name) || name.trim().isEmpty()){
                throw new IllegalStateException("Callback entry in " + yamlKey + " missing non-empty 'name'");
            }

            Optional<T> resolved = resolver.apply(name);
            if (resolved.isEmpty()) {
                throw new ConfigAgentUtils.ConfigurationException("Unknown kind " + kind + " callback name: " + name);
            }
            result.add(resolved.get());
        }
        return result;
    }


}
