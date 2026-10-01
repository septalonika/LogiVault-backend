package com.logivault.item.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemSummary(
        UUID id,
        String name,
        BigDecimal basePrice,
        boolean active,
        long variantCount,
        long totalStock,
        Instant createdAt
) {
}
