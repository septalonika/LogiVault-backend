package com.logivault.stock.dto;

import java.util.UUID;

public record StockChangeResponse(UUID variantId, String sku, int stock, MovementResponse movement) {
}
