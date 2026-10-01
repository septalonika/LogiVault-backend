package com.logivault.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Documentation-only shape of every error response (RFC 7807 plus {@code code}); never instantiated. */
@Schema(name = "Problem", description = "RFC 7807 error body with a stable machine-readable code")
public record ProblemDoc(
        @Schema(example = "about:blank") String type,
        @Schema(example = "Conflict") String title,
        @Schema(example = "409") int status,
        @Schema(example = "Not enough stock") String detail,
        @Schema(example = "/api/v1/orders") String instance,
        @Schema(example = "INSUFFICIENT_STOCK") String code,
        @Schema(description = "Only present on VALIDATION_ERROR") List<FieldErrorItem> errors
) {
}
