package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.InventoryDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryDeductionRepository extends JpaRepository<InventoryDeduction, Long> {
    Optional<InventoryDeduction> findByOrderId(Long orderId);
    Optional<InventoryDeduction> findByIdempotencyKey(String idempotencyKey);
}
