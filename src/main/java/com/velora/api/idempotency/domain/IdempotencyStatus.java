package com.velora.api.idempotency.domain;

public enum IdempotencyStatus {

    /**
     * A request holding this key is running right now.
     *
     * <p>This state is what makes a double-tap safe rather than merely detectable:
     * the second request arrives before the first has finished, so there is no stored
     * response to return yet — only the knowledge that one is coming.
     */
    IN_PROGRESS,

    /** Finished. The stored response is replayed for any repeat. */
    COMPLETED,

    /**
     * The original attempt failed.
     *
     * <p>Kept rather than deleted so a retry is allowed: a failed order should be
     * retryable with the same key, unlike a successful one.
     */
    FAILED
}
