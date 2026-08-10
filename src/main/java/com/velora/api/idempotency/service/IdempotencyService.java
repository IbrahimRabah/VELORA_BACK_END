package com.velora.api.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import com.velora.api.idempotency.domain.IdempotencyRecord;
import com.velora.api.idempotency.domain.IdempotencyStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Makes a repeated request safe.
 *
 * <p>The failure this prevents is ordinary and expensive: a customer on a weak
 * connection taps "place order", sees nothing, and taps again. Two orders, two stock
 * reservations, one refund and one apology.
 *
 * <p>The sequence:
 * <ol>
 *   <li>Claim the key by inserting a row. The unique index decides who wins — not
 *       a read-then-write check, which two simultaneous taps would both pass.</li>
 *   <li>If the claim fails, someone else has it. Either replay their stored response,
 *       or refuse because theirs is still running.</li>
 *   <li>Run the work, store the response, mark it completed.</li>
 * </ol>
 *
 * <p>The claim is committed in its own transaction, deliberately. If it shared the
 * caller's transaction, a rollback would erase the claim and let the duplicate
 * through — which is the exact thing being prevented.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    /** Long enough for any plausible retry, short enough that the table stays small. */
    private static final int RETENTION_HOURS = 24;

    private final IdempotencyStore store;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs {@code work} once per key.
     *
     * <p>When no key is supplied the work simply runs — idempotency is opt-in, and an
     * old client that does not send the header must still be able to buy something.
     *
     * @param responseType the type to deserialize a replayed response into
     */
    public <T> T execute(String key, String endpoint, Object requestBody, Long userId,
                         Class<T> responseType, Supplier<T> work) {

        if (key == null || key.isBlank()) {
            return work.get();
        }

        String requestHash = hash(requestBody);

        Optional<IdempotencyRecord> claimed = store.claim(key, endpoint, requestHash, userId);

        if (claimed.isEmpty()) {
            // Someone already holds this key.
            return replayOrReject(key, endpoint, requestHash, responseType);
        }

        IdempotencyRecord record = claimed.get();

        try {
            T result = work.get();
            store.complete(record.getId(), serialize(result), 201);
            return result;

        } catch (RuntimeException ex) {
            store.markFailed(record.getId());
            throw ex;
        }
    }

    /**
     * Handles a key someone else already holds.
     *
     * <p>Three outcomes, and they are genuinely different: replay a finished
     * response, refuse because one is still running, or refuse because the first
     * attempt failed and this key is spent.
     */
    private <T> T replayOrReject(String key, String endpoint, String requestHash,
                                 Class<T> responseType) {
        IdempotencyRecord existing = store.find(key, endpoint)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Idempotency key state is inconsistent"));

        // Same key, different body. That is a client bug, and replaying the first
        // response would hide it — the caller thinks they sent B and got B back.
        if (existing.getRequestHash() != null
                && !existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "This Idempotency-Key was already used with a different request");
        }

        if (existing.isInProgress()) {
            // The first request has not finished. Refusing is better than returning a
            // half-built order; the client retries in a moment.
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER,
                    "This request is already being processed. Please wait.");
        }

        if (existing.getStatus() == IdempotencyStatus.FAILED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "The original request failed. Retry with a new Idempotency-Key.");
        }

        log.info("Replaying stored response for idempotency key {} on {}", key, endpoint);
        return deserialize(existing.getResponseBody(), responseType);
    }

    // ------------------------------------------------------------------ upkeep

    public int purgeExpired() {
        return store.purgeExpired();
    }

    // ------------------------------------------------------------------ helpers

    private String hash(Object body) {
        if (body == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            // A missing hash only weakens the mismatch check; it must not block the
            // order.
            log.warn("Could not hash request body for idempotency", ex);
            return null;
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.error("Could not serialize response for idempotency replay", ex);
            return null;
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        if (json == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "The original response was not stored. Retry with a new key.");
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.error("Could not replay stored response", ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Could not replay the original response");
        }
    }
}
