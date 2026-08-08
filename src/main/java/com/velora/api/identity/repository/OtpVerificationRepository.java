package com.velora.api.identity.repository;

import com.velora.api.identity.domain.OtpPurpose;
import com.velora.api.identity.domain.OtpVerification;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /** The newest unconsumed code for this destination and purpose. */
    Optional<OtpVerification> findFirstByDestinationAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String destination, OtpPurpose purpose);

    /** Rate limiting: how many codes were requested recently. */
    @Query("""
            select count(o) from OtpVerification o
            where o.destination = :destination and o.createdAt > :since
            """)
    long countRecentRequests(@Param("destination") String destination,
                             @Param("since") OffsetDateTime since);

    /** Invalidate previous codes when a new one is issued. */
    @Modifying
    @Query("""
            update OtpVerification o set o.consumedAt = :now
            where o.destination = :destination and o.purpose = :purpose and o.consumedAt is null
            """)
    int consumeOutstanding(@Param("destination") String destination,
                           @Param("purpose") OtpPurpose purpose,
                           @Param("now") OffsetDateTime now);
}
