package com.logivault.exception;

import com.logivault.dto.WebResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns every exception into a {@link WebResponse} error body with a stable {@code code}.
 * Spring MVC's own exceptions (bad JSON, wrong method, type mismatch, ...) come through
 * {@link ResponseEntityExceptionHandler} and are converted in {@link #handleExceptionInternal}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** DB constraint name → business error, for violations that slip past service-level checks. */
    private static final Map<String, ErrorCode> CONSTRAINT_CODES = Map.of(
            "variants_sku_key", ErrorCode.SKU_ALREADY_EXISTS,
            "ux_users_email", ErrorCode.EMAIL_ALREADY_EXISTS,
            "variants_stock_check", ErrorCode.INSUFFICIENT_STOCK
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex) {
        Map<String, Object> details = ex.getProperties().isEmpty() ? null : ex.getProperties();
        return error(ex.getErrorCode(), ex.getMessage(), null, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorItem(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return validationError(errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        String constraint = constraintName(ex);
        ErrorCode code = constraint == null ? null : CONSTRAINT_CODES.get(constraint);
        if (code == null) {
            log.error("Unmapped data integrity violation (constraint={})", constraint, ex);
            code = ErrorCode.INTERNAL_ERROR;
        }
        return error(code);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return error(ErrorCode.CONCURRENT_MODIFICATION);
    }

    /** Unknown field in {@code ?sort=}. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Object> handlePropertyReference(PropertyReferenceException ex) {
        return validationError(List.of(new FieldErrorItem("sort", "Unknown property: " + ex.getPropertyName())));
    }

    /** {@code @PreAuthorize} failures raised inside controllers. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return error(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex) {
        return error(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error on {}", path(request), ex);
        return error(ErrorCode.INTERNAL_ERROR);
    }

    // ---- Spring MVC exceptions -------------------------------------------------------------

    /** {@code @Valid @RequestBody} failures. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<FieldErrorItem> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.add(new FieldErrorItem(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errors.add(new FieldErrorItem(e.getObjectName(), e.getDefaultMessage())));
        return validationError(errors);
    }

    /** Constraint annotations on {@code @PathVariable} / {@code @RequestParam}. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldErrorItem> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                errors.add(new FieldErrorItem(result.getMethodParameter().getParameterName(),
                        error.getDefaultMessage()))));
        return validationError(errors);
    }

    /** Every other response built by {@link ResponseEntityExceptionHandler} passes here and becomes an envelope. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        // Some handlers (e.g. 405) pass body=null and let the superclass build it; build it here to read its detail
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        if (!(body instanceof WebResponse<?>)) {
            String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                    ? problem.getDetail()
                    : ex.getMessage();
            body = WebResponse.error(statusCode.value(), message, codeFor(statusCode), null, null);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // ---- helpers -----------------------------------------------------------------------------

    private static ResponseEntity<Object> validationError(List<FieldErrorItem> errors) {
        return error(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors, null);
    }

    private static ResponseEntity<Object> error(ErrorCode code) {
        return error(code, code.getDefaultMessage(), null, null);
    }

    private static ResponseEntity<Object> error(ErrorCode code, String message, List<FieldErrorItem> errors,
                                                Map<String, Object> details) {
        return ResponseEntity.status(code.getStatus())
                .body(WebResponse.error(code.getStatus().value(), message, code.name(), errors, details));
    }

    /** Generic Spring MVC errors: 400 → VALIDATION_ERROR, 500 → INTERNAL_ERROR, others → HTTP status name. */
    private static String codeFor(HttpStatusCode statusCode) {
        if (statusCode.value() == 400) return ErrorCode.VALIDATION_ERROR.name();
        if (statusCode.is5xxServerError()) return ErrorCode.INTERNAL_ERROR.name();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.name() : "HTTP_" + statusCode.value();
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : "";
    }

    private static String constraintName(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof org.hibernate.exception.ConstraintViolationException cve) {
                return cve.getConstraintName();
            }
        }
        return null;
    }
}
