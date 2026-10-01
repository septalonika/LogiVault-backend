package com.logivault.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@Schema(example = "false") @NotNull Boolean active) {
}
