package com.logivault.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockInRequest(
        @Schema(example = "50") @NotNull @Positive Integer qty,
        @Schema(example = "Supplier delivery") @Size(max = 255) String note
) {
}
