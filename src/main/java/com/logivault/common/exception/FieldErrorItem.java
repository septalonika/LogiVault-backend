package com.logivault.common.exception;

/** One entry of the {@code errors[]} array in a 400 VALIDATION_ERROR response. */
public record FieldErrorItem(String field, String message) {
}
