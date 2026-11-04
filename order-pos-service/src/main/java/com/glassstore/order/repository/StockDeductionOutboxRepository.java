package com.glassstore.order.repository;

import com.glassstore.order.entity.StockDeductionOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockDeductionOutboxRepository extends JpaRepository<StockDeductionOutbox, Long> {
    @org.springframework.data.jpa.repository.Query("select x.orderId from StockDeductionOutbox x where x.id=:id")
    Optional<Long> findOrderIdById(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select x from StockDeductionOutbox x where x.id=:id")
    Optional<StockDeductionOutbox> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<StockDeductionOutbox> findByOrderId(Long orderId);
    List<StockDeductionOutbox> findTop20ByStatusOrderByCreatedAtAsc(String status);
}
