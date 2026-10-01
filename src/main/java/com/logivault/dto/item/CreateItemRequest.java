package com.logivault.dto.item;

import com.logivault.dto.variant.CreateVariantRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateItemRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal basePrice,
        @Valid List<CreateVariantRequest> variants
) {
}
