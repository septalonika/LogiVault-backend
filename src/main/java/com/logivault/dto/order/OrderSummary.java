package com.logivault.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import com.logivault.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummary(
        @Schema(example = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b") UUID id,
        @Schema(example = "ORD-20261001-0001") String code,
        OrderStatus status,
        @Schema(example = "150000.00") BigDecimal total,
        @Schema(example = "2") int itemCount,
        OrderResponse.OrderActor createdBy,
        Instant createdAt
) {
}
