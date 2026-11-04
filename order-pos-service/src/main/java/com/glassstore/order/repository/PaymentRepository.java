package com.glassstore.order.repository;

import com.glassstore.order.domain.PaymentStatus;
import com.glassstore.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /** BR-15 guard: is there already a verified success for this order? */
    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, PaymentStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findByProviderAndExternalTransactionId(String provider, String externalTransactionId);

    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
