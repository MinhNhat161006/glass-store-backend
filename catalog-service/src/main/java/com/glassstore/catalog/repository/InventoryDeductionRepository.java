package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.InventoryDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryDeductionRepository extends JpaRepository<InventoryDeduction, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryDeduction> findByOrderId(Long orderId);
    Optional<InventoryDeduction> findByIdempotencyKey(String idempotencyKey);
}
