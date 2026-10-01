package com.logivault.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty @Size(max = 50) List<@Valid OrderLineRequest> lines,
        @Size(max = 255) String note
) {
}
