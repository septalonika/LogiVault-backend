package com.logivault.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Schema(example = "Customer changed their mind") @NotBlank @Size(max = 255) String reason
) {
}
