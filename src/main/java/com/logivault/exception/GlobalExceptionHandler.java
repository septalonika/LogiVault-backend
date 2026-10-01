package com.logivault.exception;

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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns every exception into an RFC 7807 {@link ProblemDetail} with an extra stable {@code code} field.
 * Spring MVC's own exceptions (bad JSON, wrong method, type mismatch, ...) come through
 * {@link ResponseEntityExceptionHandler} and get their {@code code} in {@link #handleExceptionInternal}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String ERRORS = "errors";

    /** DB constraint name → business error, for violations that slip past service-level checks. */
    private static final Map<String, ErrorCode> CONSTRAINT_CODES = Map.of(
            "variants_sku_key", ErrorCode.SKU_ALREADY_EXISTS,
            "ux_users_email", ErrorCode.EMAIL_ALREADY_EXISTS,
            "variants_stock_check", ErrorCode.INSUFFICIENT_STOCK
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex, WebRequest request) {
        ProblemDetail problem = problem(ex.getErrorCode(), ex.getMessage(), request);
        ex.getProperties().forEach(problem::setProperty);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorItem(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return validationError(errors, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        String constraint = constraintName(ex);
        ErrorCode code = constraint == null ? null : CONSTRAINT_CODES.get(constraint);
        if (code == null) {
            log.error("Unmapped data integrity violation (constraint={})", constraint, ex);
            code = ErrorCode.INTERNAL_ERROR;
        }
        ProblemDetail problem = problem(code, code.getDefaultMessage(), request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
        ProblemDetail problem = problem(ErrorCode.CONCURRENT_MODIFICATION,
                ErrorCode.CONCURRENT_MODIFICATION.getDefaultMessage(), request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    /** Unknown field in {@code ?sort=}. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Object> handlePropertyReference(PropertyReferenceException ex, WebRequest request) {
        return validationError(List.of(new FieldErrorItem("sort", "Unknown property: " + ex.getPropertyName())), request);
    }

    /** {@code @PreAuthorize} failures raised inside controllers. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problem = problem(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage(), request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        ProblemDetail problem = problem(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage(), request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error on {}", path(request), ex);
        ProblemDetail problem = problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
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
        return validationError(errors, request);
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
        return validationError(errors, request);
    }

    /** Every response built by {@link ResponseEntityExceptionHandler} passes here: add {@code code} + {@code instance}. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        // Some handlers (e.g. 405) pass body=null and let the superclass build it; build it here so we can enrich it
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        if (body instanceof ProblemDetail problem) {
            if (problem.getProperties() == null || !problem.getProperties().containsKey(CODE)) {
                problem.setProperty(CODE, codeFor(statusCode));
            }
            if (problem.getInstance() == null) {
                problem.setInstance(URI.create(path(request)));
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // ---- helpers -----------------------------------------------------------------------------

    private ResponseEntity<Object> validationError(List<FieldErrorItem> errors, WebRequest request) {
        ProblemDetail problem = problem(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request);
        problem.setProperty(ERRORS, errors);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static ProblemDetail problem(ErrorCode code, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
        problem.setTitle(code.getStatus().getReasonPhrase());
        problem.setInstance(URI.create(path(request)));
        problem.setProperty(CODE, code.name());
        return problem;
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
