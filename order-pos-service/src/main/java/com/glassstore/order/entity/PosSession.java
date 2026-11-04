package com.glassstore.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pos_sessions", indexes = {
        @Index(name = "idx_pos_sessions_code", columnList = "session_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_code", nullable = false, unique = true, length = 60)
    private String sessionCode;

    @Column(name = "staff_name", nullable = false, length = 120)
    private String staffName;

    @Column(name = "branch_name", length = 120)
    private String branchName;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "opened_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime openedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
