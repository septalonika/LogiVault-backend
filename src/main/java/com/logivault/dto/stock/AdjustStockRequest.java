package com.logivault.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustStockRequest(
        @Schema(example = "-3") @NotNull Integer delta,
        @Schema(example = "Damaged in storage") @NotBlank @Size(max = 255) String reason
) {
}
