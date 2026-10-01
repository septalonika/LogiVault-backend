package com.logivault.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Thrown by services for any expected business failure. Rendered by {@link GlobalExceptionHandler}
 * as an error envelope with {@code code}; the optional {@code properties} (e.g. {@code lines}) go to {@code details}.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, Object> properties;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), Map.of());
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, Map.of());
    }

    public BusinessException(ErrorCode errorCode, String detail, Map<String, Object> properties) {
        super(detail);
        this.errorCode = errorCode;
        this.properties = Map.copyOf(properties);
    }
}
