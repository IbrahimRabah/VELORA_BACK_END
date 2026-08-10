package com.velora.api.idempotency.service;

import com.velora.api.idempotency.domain.IdempotencyRecord;
import com.velora.api.idempotency.domain.IdempotencyStatus;
import com.velora.api.idempotency.repository.IdempotencyRecordRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional half of idempotency, in its own bean on purpose.
 *
 * <p>Spring's {@code @Transactional} works through a proxy, so a method calling
 * another method on the SAME object bypasses it entirely — the annotation is simply
 * ignored. Keeping these three operations in a separate bean is what makes
 * {@code REQUIRES_NEW} actually take effect.
 *
 * <p>That independence is the whole point. The claim must survive a rollback of the
 * caller's transaction: if the order fails and takes the claim down with it, the
 * duplicate tap sails straight through — precisely the failure being prevented.
 */
@Service
public class IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyStore.class);
    private static final int RETENTION_HOURS = 24;

    private final IdempotencyRecordRepository repository;

    public IdempotencyStore(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Inserts the key, or returns empty if another request already holds it.
     *
     * <p>The unique index arbitrates. Checking for the key and then inserting leaves
     * a window that two simultaneous taps both pass through — the insert has to be
     * the test.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> claim(String key, String endpoint,
                                             String requestHash, Long userId) {
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setKey(key);
            record.setEndpoint(endpoint);
            record.setRequestHash(requestHash);
            record.setUserId(userId);
            record.setStatus(IdempotencyStatus.IN_PROGRESS);
            record.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(RETENTION_HOURS));

            return Optional.of(repository.saveAndFlush(record));

        } catch (DataIntegrityViolationException ex) {
            log.debug("Idempotency key {} already claimed on {}", key, endpoint);
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyRecord> find(String key, String endpoint) {
        return repository.findByKeyAndEndpoint(key, endpoint);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long recordId, String responseBody, int responseStatus) {
        repository.findById(recordId).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponseBody(responseBody);
            record.setResponseStatus(responseStatus);
            record.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.save(record);
        });
    }

    /**
     * Marks the attempt failed rather than deleting it.
     *
     * <p>A failed order should be retryable with the same key; a successful one must
     * not be. Deleting the row would make both look identical.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long recordId) {
        repository.findById(recordId).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.save(record);
        });
    }

    @Transactional
    public int purgeExpired() {
        return repository.deleteExpired(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
