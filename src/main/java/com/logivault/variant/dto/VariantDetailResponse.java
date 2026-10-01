package com.logivault.variant.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record VariantDetailResponse(
        UUID id,
        String sku,
        String name,
        Map<String, String> attributes,
        BigDecimal price,
        BigDecimal effectivePrice,
        int stock,
        int minStock,
        boolean lowStock,
        boolean active,
        UUID itemId,
        String itemName,
        boolean itemActive
) {
}
