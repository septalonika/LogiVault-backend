package com.logivault.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderLineRequest(
        @NotNull UUID variantId,
        @NotNull @Positive Integer qty
) {
}
