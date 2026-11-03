package com.glassstore.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "inventory_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "location_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;
}
