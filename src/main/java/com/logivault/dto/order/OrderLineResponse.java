package com.logivault.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
        @Schema(example = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b") UUID variantId,
        @Schema(example = "KAOS-M-HITAM") String sku,
        @Schema(example = "Kaos Polos") String itemName,
        @Schema(example = "M / Hitam") String variantName,
        @Schema(example = "2") int qty,
        @Schema(example = "75000.00") BigDecimal unitPrice,
        @Schema(example = "150000.00") BigDecimal subtotal
) {
}
