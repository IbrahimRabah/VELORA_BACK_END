package com.velora.api.invoice.repository;

import com.velora.api.invoice.domain.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    /** Scoped by customer — an invoice belongs to exactly one buyer. */
    @Query("""
            select i from Invoice i
            where i.invoiceNumber = :invoiceNumber
              and i.order.customer.id = :customerId
            """)
    Optional<Invoice> findByNumberAndCustomer(@Param("invoiceNumber") String invoiceNumber,
                                              @Param("customerId") Long customerId);

    Page<Invoice> findAllByOrderByIssuedAtDesc(Pageable pageable);

    /**
     * Delivered orders that were never invoiced.
     *
     * <p>The safety net. Auto-issue runs inside the delivery transaction, so a
     * failure there rolls the delivery back — but a manual database edit, or an
     * order delivered before this module existed, still leaves a hole. This finds it.
     */
    @Query("""
            select o.id from CustomerOrder o
            where o.fulfillmentStatus = com.velora.api.order.domain.FulfillmentStatus.DELIVERED
              and not exists (select 1 from Invoice i where i.order.id = o.id)
            order by o.deliveredAt asc
            """)
    List<Long> findDeliveredOrderIdsWithoutInvoice();

    @Query("select coalesce(max(i.sequenceNumber), 0) from Invoice i where i.fiscalYear = :year")
    int highestSequenceFor(@Param("year") int year);
}
