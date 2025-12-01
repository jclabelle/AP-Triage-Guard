package tools;

import com.google.adk.tools.Annotations;

import java.util.List;
import java.util.Map;

/**
 * Simple in-memory repository of invoices.
 *
 * For the capstone, we hard-code a tiny dataset.
 * A production program would call an ERP / finance system instead.
 */
public final class InvoiceRepoTool {

    // Fake invoice data keyed by invoice ID
    private static final Map<String, Map<String, Object>> INVOICES =
        Map.of(
            "INV-1001",
            Map.of(
                "invoice_id", "INV-1001",
                "vendor", "Acme Supplies",
                "po_number", "PO-2001",
                "currency", "USD",
                "total", 1200.0,
                "lines",
                    List.of(
                        Map.of(
                            "line_number", 1,
                            "description", "Office chairs",
                            "quantity", 10,
                            "unit_price", 100.0
                        ),
                        Map.of(
                            "line_number", 2,
                            "description", "Delivery",
                            "quantity", 1,
                            "unit_price", 200.0
                        )
                    )
            ),
            "INV-2001",
            Map.of(
                "invoice_id", "INV-2001",
                "vendor", "Acme Supplies",
                "po_number", "PO-2001",
                "currency", "USD",
                "total", 1500.0, // deliberately higher total for REVIEW scenario
                "lines",
                    List.of(
                        Map.of(
                            "line_number", 1,
                            "description", "Office chairs",
                            "quantity", 10,
                            "unit_price", 120.0
                        ),
                        Map.of(
                            "line_number", 2,
                            "description", "Delivery",
                            "quantity", 1,
                            "unit_price", 300.0
                        )
                    )
            )
        );

    private InvoiceRepoTool() {}

    /**
     * Tool entrypoint: returns invoice details for a given invoice ID.
     */
    public static Map<String, Object> getInvoice(
            @Annotations.Schema(
                    name = "invoiceId",
                    description = "The invoice ID to look up."
            )
            String invoiceId) {
        Map<String, Object> invoice = INVOICES.get(invoiceId);
        if (invoice == null) {
            return Map.of(
                "status", "not_found",
                "invoice_id", invoiceId
            );
        }
        return Map.of(
            "status", "ok",
            "invoice", invoice
        );
    }
}
