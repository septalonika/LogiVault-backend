package com.logivault.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty @Size(max = 50) List<@Valid OrderLineRequest> lines,
        @Schema(example = "Pickup at 3pm") @Size(max = 255) String note
) {
}
