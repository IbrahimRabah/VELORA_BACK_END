package com.velora.api.idempotency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One client-supplied key, and the response it produced.
 *
 * <p>Solves a specific failure: a customer taps "place order" on a slow connection,
 * sees nothing happen, and taps again. Without this the second tap creates a second
 * order — real stock reserved, a real invoice eventually, and an apology.
 *
 * <p>The client generates a UUID per checkout attempt and sends it as
 * {@code Idempotency-Key}. Same key means same intent, and the same answer comes back.
 */
@Entity
@Table(name = "idempotency_key")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String key;

    /** Scoped per endpoint, so the same key on a different action is not confused. */
    @Column(name = "endpoint", nullable = false, length = 200)
    private String endpoint;

    /**
     * Hash of the request body.
     *
     * <p>Guards against the same key being reused with different content — that is a
     * client bug, and silently returning the first response would hide it.
     */
    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status = IdempotencyStatus.IN_PROGRESS;

    /** The serialized response, replayed verbatim on a repeat. */
    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    /** Set when a caller is identified, for support lookups. */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /** Records are pruned after this. A key is only useful while a retry is likely. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    public boolean isCompleted() {
        return status == IdempotencyStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return status == IdempotencyStatus.IN_PROGRESS;
    }
}
