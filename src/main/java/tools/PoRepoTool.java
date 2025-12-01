package tools;

import com.google.adk.tools.Annotations;

import java.util.List;
import java.util.Map;

 // Simple in-memory repository of purchase orders keyed by invoice ID.
 // In a real system this would be keyed by PO number and queried via an ERP API.
public final class PoRepoTool {

    // Map invoice ID -> PO data
    private static final Map<String, Map<String, Object>> PO_BY_INVOICE_ID =
        Map.of(
            "INV-1001",
            Map.of(
                "po_number", "PO-2001",
                "vendor", "Acme Supplies",
                "currency", "USD",
                "max_total", 1200.0,
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
                "po_number", "PO-2001",
                "vendor", "Acme Supplies",
                "currency", "USD",
                "max_total", 1200.0,
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
            )
        );

    private PoRepoTool() {}

    /**
     * Tool entrypoint: returns PO details for a given invoice ID.
     */
    public static Map<String, Object> getPoForInvoice(
            @Annotations.Schema(
                    name = "invoiceId",
                    description = "The invoice ID whose PO should be loaded."
            )
            String invoiceId) {
        Map<String, Object> po = PO_BY_INVOICE_ID.get(invoiceId);
        if (po == null) {
            return Map.of(
                "status", "not_found",
                "invoice_id", invoiceId
            );
        }
        return Map.of(
            "status", "ok",
            "po", po
        );
    }
}
