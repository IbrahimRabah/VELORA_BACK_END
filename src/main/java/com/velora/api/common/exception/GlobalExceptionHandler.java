package com.velora.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Turns every exception into one consistent RFC 7807 response shape, so the Angular
 * app can implement a single HTTP error interceptor instead of handling each
 * endpoint's quirks.
 *
 * <pre>
 * {
 *   "type": "https://velora.com/errors/stock-unavailable",
 *   "title": "Conflict",
 *   "status": 409,
 *   "code": "STOCK_UNAVAILABLE",
 *   "detail": "Only 1 unit of VLR-WM-042-GLD remains",
 *   "instance": "/api/v1/orders",
 *   "timestamp": "2026-08-08T12:04:11Z"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE = "https://velora.com/errors/";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        // Business rules are expected. Log without a stack trace.
        log.warn("Business rule [{}] on {}: {}", code, request.getRequestURI(), ex.getMessage());
        return build(code, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        ProblemDetail problem = build(ErrorCode.VALIDATION_FAILED,
                "One or more fields are invalid", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex,
                                              HttpServletRequest request) {
        log.warn("Optimistic lock conflict on {}", request.getRequestURI());
        return build(ErrorCode.CONCURRENT_STOCK_CHANGE, null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex,
                                            HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, null, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex,
                                              HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, null, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ProblemDetail handleNoHandler(NoHandlerFoundException ex,
                                         HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "No endpoint matches this path", request);
    }

    /**
     * Anything unplanned. Logged at ERROR with the stack trace, but the client is
     * told nothing about internals — messages can leak table names and file paths.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, null, request);
    }

    private ProblemDetail build(ErrorCode code, String detail, HttpServletRequest request) {
        HttpStatus status = code.getStatus();
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(ERROR_BASE + code.name().toLowerCase().replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(detail != null ? detail : code.getDefaultMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }

    /** One invalid field, as reported to the client. */
    public record FieldErrorDetail(String field, String message) {
    }
}
