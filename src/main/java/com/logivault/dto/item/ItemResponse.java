package com.logivault.dto.item;

import com.logivault.dto.variant.VariantResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        boolean active,
        List<VariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {
}
