package com.logivault.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes returned in the {@code code} field of every error response.
 * Clients may translate them; never rename an existing code.
 */
@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "Old password is incorrect"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Item not found"),
    VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "Variant not found"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    SKU_ALREADY_EXISTS(HttpStatus.CONFLICT, "SKU already exists"),
    SKU_IMMUTABLE(HttpStatus.CONFLICT, "SKU cannot be changed after stock movements exist"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Not enough stock"),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "Order status does not allow this action"),
    SELF_MODIFICATION_NOT_ALLOWED(HttpStatus.CONFLICT, "You cannot deactivate or demote your own account"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "The resource was modified by another request; reload and retry"),

    VARIANT_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Item or variant is inactive"),

    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts; try again later"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
