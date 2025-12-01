package observability;

public final class MetricsHelper {

    private MetricsHelper() {}

    public static long computeLatencyMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
