package com.glassstore.lab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warranty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warranty_code", nullable = false, unique = true)
    private String warrantyCode;

    @Column(name = "original_order_code")
    private String originalOrderCode;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_tier")
    private String customerTier;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_sku")
    private String productSku;

    @Column(name = "product_details")
    private String productDetails;

    @Column(name = "issue_description", length = 1000)
    private String issueDescription;

    @Column(name = "claim_type")
    private String claimType;

    @Column(name = "policy_ref")
    private String policyRef;

    @Column(name = "tech_note", length = 1000)
    private String techNote;

    @Column(name = "cost")
    private String cost;

    @Column(name = "tech_name")
    private String techName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "status_label")
    private String statusLabel;

    @Column(name = "status_color")
    private String statusColor;

    @Column(name = "time_label")
    private String timeLabel;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
