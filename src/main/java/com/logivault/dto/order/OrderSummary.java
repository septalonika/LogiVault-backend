package com.logivault.dto.order;

import com.logivault.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummary(
        UUID id,
        String code,
        OrderStatus status,
        BigDecimal total,
        int itemCount,
        OrderResponse.OrderActor createdBy,
        Instant createdAt
) {
}
