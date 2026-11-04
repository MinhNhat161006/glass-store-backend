package com.glassstore.order.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.glassstore.order.domain.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Append-only record of every status change (SRS §4.6: timestamps, actor, history).
 *
 * <p>Written only by {@code OrderStateMachineService}; never updated in place, so the audit
 * trail survives any later correction (BR-45).</p>
 */
@Entity
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_order_history_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private OrderStatus toStatus;

    /** Username/employee code of the actor, or SYSTEM. */
    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}