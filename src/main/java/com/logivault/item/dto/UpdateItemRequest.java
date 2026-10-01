package com.logivault.item.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateItemRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal basePrice
) {
}
