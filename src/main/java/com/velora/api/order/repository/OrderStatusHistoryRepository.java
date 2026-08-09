package com.velora.api.order.repository;

import com.velora.api.order.domain.OrderStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
