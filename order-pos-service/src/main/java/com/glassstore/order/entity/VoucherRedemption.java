package com.glassstore.order.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="voucher_redemptions",uniqueConstraints=@UniqueConstraint(columnNames="order_id")) @Data @NoArgsConstructor
public class VoucherRedemption {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long voucherId;
 private Long customerId;
 @Column(name="order_id") private Long orderId;
 private boolean reversed=false;
 private LocalDateTime createdAt=LocalDateTime.now();
}

