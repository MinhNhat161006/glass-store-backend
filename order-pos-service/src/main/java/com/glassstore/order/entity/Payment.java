package com.glassstore.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "payment_method", nullable = false, columnDefinition = "varchar(32)")
    private String paymentMethod;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "status", columnDefinition = "varchar(32)")
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "paid_at")
    @Builder.Default
    private LocalDateTime paidAt = LocalDateTime.now();
}
