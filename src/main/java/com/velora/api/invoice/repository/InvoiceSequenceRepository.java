package com.velora.api.invoice.repository;

import com.velora.api.invoice.domain.InvoiceSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Integer> {

    /**
     * Loads the year's counter under a write lock.
     *
     * <p>The lock is the mechanism that makes the sequence gapless: two invoices
     * issued at the same instant queue here instead of both reading the same number.
     * Held only for the length of the transaction that writes the invoice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InvoiceSequence s where s.fiscalYear = :year")
    Optional<InvoiceSequence> lockForYear(@Param("year") Integer year);
}
