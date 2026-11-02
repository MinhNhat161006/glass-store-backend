package com.glassstore.lab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "optometrist_id", nullable = false)
    private Long optometristId;

    @Column(name = "optometrist_name")
    private String optometristName;

    @Column(name = "od_sph", precision = 4, scale = 2, nullable = false)
    private BigDecimal odSph;

    @Column(name = "od_cyl", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal odCyl = BigDecimal.ZERO;

    @Column(name = "od_axis")
    @Builder.Default
    private Integer odAxis = 0;

    @Column(name = "od_add", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal odAdd = BigDecimal.ZERO;

    @Column(name = "os_sph", precision = 4, scale = 2, nullable = false)
    private BigDecimal osSph;

    @Column(name = "os_cyl", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal osCyl = BigDecimal.ZERO;

    @Column(name = "os_axis")
    @Builder.Default
    private Integer osAxis = 0;

    @Column(name = "os_add", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal osAdd = BigDecimal.ZERO;

    @Column(name = "pd", precision = 4, scale = 1, nullable = false)
    private BigDecimal pd;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

