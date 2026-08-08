package com.velora.api.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A one-time code sent to a phone or email.
 *
 * <p>The code itself is never stored — only its hash, and it is single-use
 * ({@code consumedAt}). {@code attempts} caps brute force: six digits is only a
 * million combinations, which is minutes of guessing without a limit.
 */
@Entity
@Table(name = "otp_verification")
@Getter
@Setter
@NoArgsConstructor
public class OtpVerification {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    /** The phone (E.164) or email the code was sent to. */
    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private OtpChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public boolean isExpired() {
        return expiresAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC));
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean hasAttemptsLeft() {
        return attempts < MAX_ATTEMPTS;
    }

    public boolean isUsable() {
        return !isExpired() && !isConsumed() && hasAttemptsLeft();
    }

    public void recordFailedAttempt() {
        this.attempts++;
    }

    public void consume() {
        this.consumedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
