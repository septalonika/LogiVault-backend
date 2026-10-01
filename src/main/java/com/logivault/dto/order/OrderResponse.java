package com.logivault.dto.order;

import com.logivault.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String code,
        OrderStatus status,
        BigDecimal total,
        String note,
        List<OrderLineResponse> lines,
        OrderActor createdBy,
        Instant createdAt,
        OrderActor cancelledBy,
        Instant cancelledAt,
        String cancelReason
) {

    public record OrderActor(UUID id, String name) {
    }
}
