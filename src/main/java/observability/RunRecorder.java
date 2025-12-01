package observability;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import io.opentelemetry.api.common.Attributes;

// Records metrics in the SDK
public class RunRecorder {

    public void recordAgentRun(String agentName, long latencyMs, String status){
        Attributes attrs = Attributes.of(
                stringKey("agent.name"), agentName,
                stringKey("status"), status // ok or error
        );

        AgentMetrics.AGENT_RUNS.add(1, attrs);
        AgentMetrics.AGENT_LATENCY_MS.record((double)latencyMs, attrs);
    }
}
