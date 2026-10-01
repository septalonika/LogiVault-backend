package com.logivault.common.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Thrown by services for any expected business failure. Rendered by {@link GlobalExceptionHandler}
 * as a ProblemDetail with {@code code} plus the optional extra {@code properties} (e.g. {@code lines}).
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
