package com.logivault.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustStockRequest(
        @NotNull Integer delta,
        @NotBlank @Size(max = 255) String reason
) {
}
