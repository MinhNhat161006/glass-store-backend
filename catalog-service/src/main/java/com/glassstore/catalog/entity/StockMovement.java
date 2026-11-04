package com.glassstore.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit trail of every stock change (BR-10).
 *
 * <p>Rows are append-only: correcting a mistake means writing a new movement, never editing
 * the old one, so the inventory history stays trustworthy.</p>
 */
@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_stock_movement_sku", columnList = "sku"),
        @Index(name = "idx_stock_movement_reference", columnList = "reference")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    /** Signed change applied to the counter the movement targets. */
    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    /** Snapshot of available stock after applying the movement, for reconciliation. */
    @Column(name = "available_after", nullable = false)
    private Integer availableAfter;

    /** Order code, reservation key, or transfer reference this movement belongs to. */
    @Column(name = "reference", length = 120)
    private String reference;

    /** Who caused it (staff id/username, or SYSTEM). */
    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}