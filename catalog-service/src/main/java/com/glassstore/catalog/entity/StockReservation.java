package com.glassstore.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A hold on one SKU for one order.
 *
 * <p>Reservations exist so that a customer who has not paid yet does not lose the item to a
 * later shopper, while still reducing the quantity other shoppers can buy (BR-08).
 * There is deliberately no expiry column: the hold policy is an open business question
 * (OQ-03), so a reservation lives until it is committed or released.</p>
 */
@Entity
@Table(name = "stock_reservations", indexes = {
        @Index(name = "idx_reservation_key_sku", columnList = "reservation_key, sku", unique = true),
        @Index(name = "idx_reservation_order", columnList = "order_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Client/idempotency supplied key, unique so a retried checkout cannot double-hold. */
    @Column(name = "reservation_key", nullable = false, length = 120)
    private String reservationKey;

    @Column(name = "order_code", length = 60)
    private String orderCode;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}