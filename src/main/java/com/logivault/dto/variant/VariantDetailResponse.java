package com.logivault.dto.variant;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record VariantDetailResponse(
        @Schema(example = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b") UUID id,
        @Schema(example = "KAOS-M-HITAM") String sku,
        @Schema(example = "M / Hitam") String name,
        Map<String, String> attributes,
        @Schema(example = "80000.00") BigDecimal price,
        @Schema(example = "80000.00") BigDecimal effectivePrice,
        @Schema(example = "60") int stock,
        @Schema(example = "5") int minStock,
        boolean lowStock,
        boolean active,
        @Schema(example = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b") UUID itemId,
        @Schema(example = "Kaos Polos") String itemName,
        boolean itemActive
) {
}
