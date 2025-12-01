package observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;

public final class AgentMetrics {
    private static final Meter METER =
            GlobalOpenTelemetry.getMeter("your.org.adk.metrics");

    public static final LongCounter AGENT_RUNS =
            METER.counterBuilder("agent_runs_total")
                    .setDescription("Number of ADK agent runs")
                    .setUnit("1")
                    .build();

    public static final DoubleHistogram AGENT_LATENCY_MS =
            METER.histogramBuilder("agent_run_latency_ms")
                    .setDescription("Latency of ADK agent runs in ms")
                    .setUnit("ms")
                    .build();

    private AgentMetrics() {}

}
