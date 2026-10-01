package com.logivault.dto.stock;

import java.util.UUID;

public record LowStockResponse(
        UUID variantId,
        String sku,
        String itemName,
        String variantName,
        int stock,
        int minStock
) {
}
