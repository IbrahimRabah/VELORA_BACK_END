package com.velora.api.common.exception;

import java.io.Serial;

/**
 * An expected, recoverable failure — out of stock, expired coupon, invalid status
 * transition. These are NOT bugs: they are business rules being enforced.
 *
 * <p>Logged at WARN, never at ERROR, and never with a stack trace. A flood of
 * stack traces for "coupon expired" makes real incidents impossible to spot.
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /** Use when the detail helps the customer act, e.g. "Only 2 units remain". */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static BusinessException of(ErrorCode code) {
        return new BusinessException(code);
    }

    public static BusinessException of(ErrorCode code, String detail) {
        return new BusinessException(code, detail);
    }
}
