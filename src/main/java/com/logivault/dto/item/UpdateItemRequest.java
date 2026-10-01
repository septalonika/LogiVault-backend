package com.logivault.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateItemRequest(
        @Schema(example = "Kaos Polos") @NotBlank @Size(max = 150) String name,
        @Schema(example = "Cotton combed 30s") String description,
        @Schema(example = "75000.00") @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal basePrice
) {
}
