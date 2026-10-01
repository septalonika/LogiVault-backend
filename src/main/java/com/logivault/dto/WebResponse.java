package com.logivault.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.logivault.exception.FieldErrorItem;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Envelope for every response body, success or error. {@code data} is always present (null on errors and
 * on actions without a result); {@code meta}, {@code code}, {@code errors} and {@code details} only when set.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebResponse<T>(
        @Schema(example = "200") int status,
        @Schema(example = "Item retrieved") String message,
        @Schema(description = "Stable error code, only on errors", example = "INSUFFICIENT_STOCK") String code,
        @JsonInclude(JsonInclude.Include.ALWAYS) T data,
        @Schema(description = "Paging info, only on list endpoints") PageMeta meta,
        @Schema(description = "Field errors, only on VALIDATION_ERROR") List<FieldErrorItem> errors,
        @Schema(description = "Extra error context, e.g. the short lines of INSUFFICIENT_STOCK") Map<String, Object> details
) {

    public record PageMeta(
            @Schema(example = "0") int page,
            @Schema(example = "20") int size,
            @Schema(example = "42") long totalElements,
            @Schema(example = "3") int totalPages
    ) {
    }

    public static <T> WebResponse<T> of(HttpStatus status, String message, T data) {
        return new WebResponse<>(status.value(), message, null, data, null, null, null);
    }

    public static <T> WebResponse<List<T>> page(String message, PageResponse<T> page) {
        PageMeta meta = new PageMeta(page.page(), page.size(), page.totalElements(), page.totalPages());
        return new WebResponse<>(HttpStatus.OK.value(), message, null, page.content(), meta, null, null);
    }

    public static WebResponse<Void> error(int status, String message, String code,
                                          List<FieldErrorItem> errors, Map<String, Object> details) {
        return new WebResponse<>(status, message, code, null, null, errors, details);
    }

    public static <T> ResponseEntity<WebResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(of(HttpStatus.OK, message, data));
    }

    public static ResponseEntity<WebResponse<Void>> ok(String message) {
        return ok(message, null);
    }
}
