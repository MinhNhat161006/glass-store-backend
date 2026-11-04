package com.glassstore.order.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "lab_dispatch_outbox")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabDispatchOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false, unique = true) private Long orderId;
    @Builder.Default @Column(nullable = false) private String status = "PENDING";
    @Builder.Default @Column(nullable = false) private int attempts = 0;
    @Column(name = "lab_job_id") private Long labJobId;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Builder.Default @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "processed_at") private LocalDateTime processedAt;
}
