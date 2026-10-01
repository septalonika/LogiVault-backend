package com.logivault.stock.dto;

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
