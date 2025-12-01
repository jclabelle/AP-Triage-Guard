package tools;

import java.util.Map;

/**
 * Simple AP policy thresholds for variance and auto-approval.
 *
 * This is a "quick win" missing the capability to have a configurable policy
 * layer the agent can consult instead of hard-coding logic in prompts.
 */
public final class PolicyThresholdsTool {

    private PolicyThresholdsTool() {}

    /**
     * Returns thresholds used to decide whether an invoice is OK_TO_PAY vs REVIEW.
     */
    public static Map<String, Object> getThresholds() {
        return Map.of(
            "status", "ok",
            // max allowed line-level variance (percent) for quantity or unit price
            "max_line_variance_percent", 2.0,
            // max allowed invoice total variance vs PO max_total (percent)
            "max_invoice_variance_percent", 1.0,
            // max invoice amount that can be auto-approved
            "auto_approval_max_amount", 1300.0
        );
    }
}

