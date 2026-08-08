package com.velora.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Every expected failure in VELORA has a stable code here.
 *
 * <p>Angular branches on {@code code}, never on the message text — messages are
 * localized and may be reworded, codes are a contract.
 */
public enum ErrorCode {

    // ---- generic ----
    VALIDATION_FAILED("Validation failed", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("Authentication required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("You do not have permission to perform this action", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),

    // ---- identity ----
    INVALID_CREDENTIALS("Invalid phone/email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_SUSPENDED("This account has been suspended", HttpStatus.FORBIDDEN),
    EMAIL_ALREADY_EXISTS("This email is already registered", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS("This phone number is already registered", HttpStatus.CONFLICT),
    INVALID_PHONE_FORMAT("Not a valid Egyptian mobile number", HttpStatus.BAD_REQUEST),
    OTP_INVALID("The verification code is incorrect", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("The verification code has expired", HttpStatus.BAD_REQUEST),
    OTP_TOO_MANY_ATTEMPTS("Too many attempts. Request a new code", HttpStatus.TOO_MANY_REQUESTS),
    TOKEN_EXPIRED("Session expired. Please sign in again", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("Invalid authentication token", HttpStatus.UNAUTHORIZED),

    // ---- catalog ----
    PRODUCT_NOT_FOUND("Product not found", HttpStatus.NOT_FOUND),
    VARIANT_NOT_FOUND("This option is not available", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_ACTIVE("This product is not currently for sale", HttpStatus.CONFLICT),
    SKU_ALREADY_EXISTS("This SKU is already in use", HttpStatus.CONFLICT),
    SLUG_ALREADY_EXISTS("This URL slug is already in use", HttpStatus.CONFLICT),
    CATEGORY_NOT_EMPTY("Move or archive the products in this category first", HttpStatus.CONFLICT),

    // ---- inventory ----
    STOCK_UNAVAILABLE("Not enough stock available", HttpStatus.CONFLICT),
    RESERVATION_EXPIRED("Your checkout session expired. Please review your cart", HttpStatus.CONFLICT),
    CONCURRENT_STOCK_CHANGE("Stock changed while processing. Please try again", HttpStatus.CONFLICT),

    // ---- cart ----
    CART_EMPTY("Your cart is empty", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND("This item is no longer in your cart", HttpStatus.NOT_FOUND),
    PRICE_CHANGED("Prices in your cart have changed. Please review", HttpStatus.CONFLICT),

    // ---- promotion ----
    COUPON_NOT_FOUND("Coupon code not recognised", HttpStatus.NOT_FOUND),
    COUPON_EXPIRED("This coupon has expired", HttpStatus.CONFLICT),
    COUPON_USAGE_LIMIT_REACHED("This coupon has reached its usage limit", HttpStatus.CONFLICT),
    COUPON_MINIMUM_NOT_MET("Your order does not reach the minimum for this coupon", HttpStatus.CONFLICT),
    COUPON_NOT_APPLICABLE("This coupon does not apply to the items in your cart", HttpStatus.CONFLICT),

    // ---- shipping ----
    GOVERNORATE_NOT_SERVED("We do not deliver to this governorate yet", HttpStatus.CONFLICT),
    SHIPPING_RATE_NOT_CONFIGURED("Shipping is not configured for this area", HttpStatus.CONFLICT),
    INVALID_ADDRESS("The delivery address is incomplete", HttpStatus.BAD_REQUEST),

    // ---- order ----
    ORDER_NOT_FOUND("Order not found", HttpStatus.NOT_FOUND),
    INVALID_STATUS_TRANSITION("This status change is not allowed", HttpStatus.CONFLICT),
    ORDER_CANNOT_BE_CANCELLED("This order can no longer be cancelled", HttpStatus.CONFLICT),
    RETURN_WINDOW_CLOSED("The return period for this order has ended", HttpStatus.CONFLICT),
    RETURN_QUANTITY_EXCEEDED("You cannot return more than you ordered", HttpStatus.BAD_REQUEST),
    DUPLICATE_ORDER("This order has already been submitted", HttpStatus.CONFLICT),

    // ---- payment ----
    PAYMENT_METHOD_UNAVAILABLE("This payment method is not available", HttpStatus.CONFLICT),
    REFUND_EXCEEDS_ORDER_TOTAL("Refund is greater than the order total", HttpStatus.BAD_REQUEST),

    // ---- review ----
    PURCHASE_REQUIRED("Only verified buyers can review this product", HttpStatus.UNPROCESSABLE_ENTITY),
    REVIEW_ALREADY_EXISTS("You have already reviewed this product", HttpStatus.CONFLICT);

    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String defaultMessage, HttpStatus status) {
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
