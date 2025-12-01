package config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class OpenTelemetryBootstrap {

    private static final AttributeKey<String> SERVICE_NAME = AttributeKey.stringKey("service.name");
    private static final AttributeKey<String> GCP_PROJECT_ID = AttributeKey.stringKey("gcp.project_id");

    public static void init(boolean logging) {
        if (logging) {
            LoggingSpanExporter exporter = LoggingSpanExporter.create();
            initRuntime(exporter);
            return;
        }

        initGCloud();

    }

    private static void initRuntime(SpanExporter spanExporter){
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                        .build())
                .setResource(Resource.getDefault().merge(
                        Resource.create(Attributes.of(SERVICE_NAME, "minimal-agent")))
                )
                .setSampler(Sampler.traceIdRatioBased(0.02))
                .build();

        OpenTelemetrySdk openTelemetry =  OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        Runtime.getRuntime().addShutdownHook(new Thread(openTelemetry::close));
    }

    public static OpenTelemetry initGCloud(){
        // Load ADC (use GOOGLE_APPLICATION_CREDENTIALS locally)
        GoogleCredentials credentials;
        try {
            credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(
                            List.of("https://www.googleapis.com/auth/cloud-platform"));
        } catch (IOException e){
            throw new RuntimeException("Failed to load Google credentials", e);
        }

        // Determine OTLP endpoint: prefer env, else fallback to Google Telemetry API
        String endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://telemetry.googleapis.com:443";
        }

        // Create OTLP GRPC exporter with auth headers
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .setHeaders( () -> {
                    try {
                        credentials.refreshIfExpired();
                        Map<String, List<String>> googleHeaders = credentials.getRequestMetadata();
                        // Transform multi-value headers into comma-separated strings
                        return googleHeaders.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().stream()
                                                .filter(Objects::nonNull)
                                                .filter(s -> !s.isEmpty())
                                                .collect(Collectors.joining(","))));
                    } catch (IOException e){
                        throw new RuntimeException("Failed to refresh Google credentials", e);
                    }
                })
                .setTimeout(2, TimeUnit.SECONDS)
                .build();

        // Build a resource (service name + project id).
        // We manually propagate gcp.project_id from OTEL_RESOURCE_ATTRIBUTES or related env,
        // because we're not using the autoconfigure module.
        String projectId = extractProjectIdFromEnv();
        Attributes resourceAttrs =
                (projectId != null && !projectId.isBlank())
                        ? Attributes.of(ResourceAttributes.SERVICE_NAME, "adk-java-agent",
                                        GCP_PROJECT_ID, projectId)
                        : Attributes.of(ResourceAttributes.SERVICE_NAME, "adk-java-agent");

        Resource resource = Resource.getDefault().merge(Resource.create(resourceAttrs));

        // Tracer provider
        SdkTracerProvider tracerProvider =
                SdkTracerProvider.builder()
                        .setResource(resource)
                        .addSpanProcessor(
                                BatchSpanProcessor.builder(spanExporter)
                                        .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                                        .build())
                        .build();

        // Google Cloud Monitoring exporter
        MetricExporter metricsExporter = GoogleCloudMetricExporter.createWithDefaultConfiguration();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(metricsExporter)
                                .setInterval(Duration.ofSeconds(30)) // export every 30s
                                .build())
                .build();


        // Build the SDK instance
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();

    }

    private static String extractProjectIdFromEnv() {
        // Preferred: OTEL_RESOURCE_ATTRIBUTES="gcp.project_id=...,service.name=..."
        String attrs = System.getenv("OTEL_RESOURCE_ATTRIBUTES");
        if (attrs != null && !attrs.isBlank()) {
            for (String pair : attrs.split(",")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2 && "gcp.project_id".equals(parts[0].trim())) {
                    return parts[1].trim();
                }
            }
        }

        // Fallbacks commonly used in GCP environments
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId == null || projectId.isBlank()) {
            projectId = System.getenv("GCLOUD_PROJECT");
        }
        return projectId;
    }


}
