package com.velora.api.idempotency.repository;

import com.velora.api.idempotency.domain.IdempotencyRecord;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByKeyAndEndpoint(String key, String endpoint);

    /**
     * Removes records past their window.
     *
     * <p>A key only matters while a retry is plausible. Keeping them forever turns a
     * safety mechanism into an ever-growing table, and the stored response bodies are
     * not small.
     */
    @Modifying
    @Query("delete from IdempotencyRecord r where r.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
