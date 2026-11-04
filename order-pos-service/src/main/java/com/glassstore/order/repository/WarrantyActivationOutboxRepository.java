package com.glassstore.order.repository;

import com.glassstore.order.entity.WarrantyActivationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarrantyActivationOutboxRepository extends JpaRepository<WarrantyActivationOutbox, Long> {
    Optional<WarrantyActivationOutbox> findByOrderItemIdAndBenefitType(Long orderItemId, String benefitType);

    List<WarrantyActivationOutbox> findByOrderIdOrderByOrderItemIdAsc(Long orderId);

    List<WarrantyActivationOutbox> findTop20ByStatusOrderByCreatedAtAsc(String status);
}
