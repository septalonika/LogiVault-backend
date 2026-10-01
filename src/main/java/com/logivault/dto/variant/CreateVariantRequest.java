package com.logivault.dto.variant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateVariantRequest(
        @Schema(example = "KAOS-M-HITAM") @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "must contain only letters, digits, - and _") String sku,
        @Schema(example = "M / Hitam") @NotBlank @Size(max = 100) String name,
        Map<String, String> attributes,
        @Schema(example = "80000.00") @PositiveOrZero BigDecimal price,
        @Schema(example = "5") @PositiveOrZero Integer minStock
) {
}
