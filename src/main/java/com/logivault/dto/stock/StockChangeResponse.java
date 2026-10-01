package com.logivault.dto.stock;

import java.util.UUID;

public record StockChangeResponse(UUID variantId, String sku, int stock, MovementResponse movement) {
}
