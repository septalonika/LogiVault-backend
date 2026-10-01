package com.logivault.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** Documentation-only shape of every error response; never instantiated. */
@Schema(name = "ErrorResponse", description = "Error envelope with a stable machine-readable code")
public record ErrorResponseDoc(
        @Schema(example = "409") int status,
        @Schema(example = "Not enough stock") String message,
        @Schema(example = "INSUFFICIENT_STOCK") String code,
        @Schema(description = "Always null on errors", nullable = true) Object data,
        @Schema(description = "Only present on VALIDATION_ERROR") List<FieldErrorItem> errors,
        @Schema(description = "Extra context for some codes, e.g. lines for INSUFFICIENT_STOCK") Map<String, Object> details
) {
}
