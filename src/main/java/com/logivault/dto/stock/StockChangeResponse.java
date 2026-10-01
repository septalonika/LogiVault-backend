package com.logivault.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record StockChangeResponse(UUID variantId, @Schema(example = "KAOS-M-HITAM") String sku, @Schema(example = "60") int stock, MovementResponse movement) {
}
