package com.glassstore.order.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="loyalty_accounts") @Data @NoArgsConstructor
public class LoyaltyAccount {
 @Id private Long customerId;
 @Column(nullable=false) private long balance=0;
 @Version private Long version;
}

