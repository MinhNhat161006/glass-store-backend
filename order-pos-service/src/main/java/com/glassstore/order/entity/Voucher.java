package com.glassstore.order.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="vouchers") @Data @NoArgsConstructor
public class Voucher {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=50) private String code;
 private String type;
 @Column(precision=12,scale=2) private BigDecimal amount;
 @Column(precision=12,scale=2) private BigDecimal minimumAmount=BigDecimal.ZERO;
 @Column(precision=12,scale=2) private BigDecimal maximumDiscount;
 private boolean active=true;
 private boolean membersOnly=false;
 private boolean stackPoints=true;
 private LocalDateTime startsAt;
 private LocalDateTime expiresAt;
 private Integer usageLimit;
 private Integer perCustomerLimit;
 private int usedCount=0;
}

