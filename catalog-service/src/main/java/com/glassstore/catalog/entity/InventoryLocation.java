package com.glassstore.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
