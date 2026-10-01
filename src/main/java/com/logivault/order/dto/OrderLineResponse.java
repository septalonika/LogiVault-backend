package com.logivault.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
        UUID variantId,
        String sku,
        String itemName,
        String variantName,
        int qty,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
