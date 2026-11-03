package com.glassstore.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "channel")
    @Builder.Default
    private String channel = "POS";

    @Column(name = "order_type")
    @Builder.Default
    private String orderType = "COMPLETE_GLASSES";

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "status")
    @Builder.Default
    private String status = "PAID";

    @Column(name = "payment_method")
    @Builder.Default
    private String paymentMethod = "VIETQR";

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "branch_name")
    @Builder.Default
    private String branchName = "Flagship Q.1 (TP.HCM)";

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Transient
    private String inventorySyncStatus;
}
