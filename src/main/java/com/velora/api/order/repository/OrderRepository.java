package com.velora.api.order.repository;

import com.velora.api.order.domain.CustomerOrder;
import com.velora.api.order.domain.FulfillmentStatus;
import com.velora.api.order.domain.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository
        extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    /**
     * Scoped by customer. Looking up by order number alone would let anyone read
     * anyone's order by guessing — which is exactly why the number is not sequential.
     */
    @EntityGraph(attributePaths = {"items"})
    Optional<CustomerOrder> findByOrderNumberAndCustomerId(String orderNumber, Long customerId);

    @EntityGraph(attributePaths = {"items"})
    Optional<CustomerOrder> findWithItemsById(Long id);

    Page<CustomerOrder> findByCustomerIdOrderByPlacedAtDesc(Long customerId, Pageable pageable);

    Page<CustomerOrder> findByFulfillmentStatusOrderByPlacedAtDesc(
            FulfillmentStatus status, Pageable pageable);

    /** Support lookup — the customer rings and gives a phone number. */
    Page<CustomerOrder> findByContactPhoneOrderByPlacedAtDesc(String phone, Pageable pageable);

    /**
     * Delivered COD orders whose cash has not been remitted by the courier yet.
     * Until an order appears in a remittance, its money has NOT arrived.
     */
    @Query("""
            select o from CustomerOrder o
            where o.paymentMethod = com.velora.api.order.domain.PaymentMethod.COD
              and o.fulfillmentStatus = com.velora.api.order.domain.FulfillmentStatus.DELIVERED
              and o.paymentStatus = com.velora.api.order.domain.PaymentStatus.PENDING
            order by o.deliveredAt asc
            """)
    List<CustomerOrder> findUnsettledCodOrders();

    @Query("""
            select count(o) from CustomerOrder o
            where o.fulfillmentStatus = :status
            """)
    long countByStatus(@Param("status") FulfillmentStatus status);

    @Query("""
            select coalesce(sum(o.grandTotal), 0) from CustomerOrder o
            where o.placedAt >= :from
              and o.fulfillmentStatus <> com.velora.api.order.domain.FulfillmentStatus.CANCELLED
            """)
    java.math.BigDecimal revenueSince(@Param("from") OffsetDateTime from);

    long countByPaymentStatus(PaymentStatus status);
}
