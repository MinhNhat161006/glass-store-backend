package com.glassstore.lab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lab_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "frame_sku", nullable = false, length = 100)
    private String frameSku;

    @Column(name = "frame_name")
    private String frameName;

    @Column(name = "lens_sku", nullable = false, length = 100)
    private String lensSku;

    @Column(name = "lens_name")
    private String lensName;

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "technician_name")
    private String technicianName;

    @Column(length = 50)
    @Builder.Default
    private String status = "WAITING_PROCESS"; // WAITING_PROCESS, IN_PROGRESS, QC_CHECK, QC_PASSED, QC_FAILED, READY_FOR_DELIVERY

    @Column(name = "qc_notes", length = 500)
    private String qcNotes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

