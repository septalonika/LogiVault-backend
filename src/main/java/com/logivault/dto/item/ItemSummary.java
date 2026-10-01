package com.logivault.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemSummary(
        @Schema(example = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b") UUID id,
        @Schema(example = "Kaos Polos") String name,
        @Schema(example = "75000.00") BigDecimal basePrice,
        boolean active,
        @Schema(example = "3") long variantCount,
        @Schema(example = "120") long totalStock,
        Instant createdAt
) {
}
