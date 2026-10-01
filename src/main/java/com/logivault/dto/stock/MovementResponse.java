package com.logivault.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import com.logivault.entity.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        @Schema(example = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b") UUID id,
        MovementType type,
        @Schema(example = "50") int qty,
        @Schema(example = "10") int stockBefore,
        @Schema(example = "60") int stockAfter,
        @Schema(example = "Supplier delivery") String reason,
        @Schema(example = "ORD-20261001-0001") String orderCode,
        MovementActor actor,
        Instant createdAt
) {

    public record MovementActor(UUID id, String name) {
    }
}
