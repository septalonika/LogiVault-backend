package com.logivault.stock.dto;

import com.logivault.stock.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        MovementType type,
        int qty,
        int stockBefore,
        int stockAfter,
        String reason,
        String orderCode,
        MovementActor actor,
        Instant createdAt
) {

    public record MovementActor(UUID id, String name) {
    }
}
