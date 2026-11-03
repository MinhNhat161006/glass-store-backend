package com.glassstore.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_deduction_outbox",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "order_id"),
                @UniqueConstraint(columnNames = "idempotency_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDeductionOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, length = 24)
    @Builder.Default
    private String status = "PENDING";

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
