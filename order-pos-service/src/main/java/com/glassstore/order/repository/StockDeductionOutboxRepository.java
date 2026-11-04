package com.glassstore.order.repository;

import com.glassstore.order.entity.StockDeductionOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockDeductionOutboxRepository extends JpaRepository<StockDeductionOutbox, Long> {
    Optional<StockDeductionOutbox> findByOrderId(Long orderId);
    List<StockDeductionOutbox> findTop20ByStatusOrderByCreatedAtAsc(String status);
}
