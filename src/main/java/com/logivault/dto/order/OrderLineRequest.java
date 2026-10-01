package com.logivault.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderLineRequest(
        @NotNull UUID variantId,
        @NotNull @Positive Integer qty
) {
}
