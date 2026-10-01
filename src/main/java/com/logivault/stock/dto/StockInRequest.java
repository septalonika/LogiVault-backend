package com.logivault.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockInRequest(
        @NotNull @Positive Integer qty,
        @Size(max = 255) String note
) {
}
