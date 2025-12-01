package eval.model.results;

public record MetricScore(
        String metricName,
        double score,
        Double threshold,   // null if not configured
        boolean passed
) {}
