package com.logivault.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record LowStockResponse(
        @Schema(example = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b") UUID variantId,
        @Schema(example = "KAOS-M-HITAM") String sku,
        @Schema(example = "Kaos Polos") String itemName,
        @Schema(example = "M / Hitam") String variantName,
        @Schema(example = "2") int stock,
        @Schema(example = "5") int minStock
) {
}
