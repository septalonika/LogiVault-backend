package com.logivault.variant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateVariantRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "must contain only letters, digits, - and _") String sku,
        @NotBlank @Size(max = 100) String name,
        Map<String, String> attributes,
        @PositiveOrZero BigDecimal price,
        @PositiveOrZero Integer minStock
) {
}
