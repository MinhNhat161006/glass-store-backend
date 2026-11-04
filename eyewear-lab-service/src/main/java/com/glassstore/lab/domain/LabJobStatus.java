package com.glassstore.lab.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lens fabrication lifecycle (SRS §4.11, BR-29..BR-34).
 *
 * <p>QC is a gate, not a suggestion: {@code READY_FOR_DELIVERY} is only reachable from
 * {@code QC_PASSED}, so a job can never be handed over without passing inspection.</p>
 */
public enum LabJobStatus {

    /** Created, not yet accepted by the workshop. */
    WAITING_PROCESS,
    /** Workshop accepted the job. */
    RECEIVED,
    /** Being ground/edged. */
    IN_PROGRESS,
    /** Awaiting quality inspection. */
    QC_CHECK,
    /** Inspection passed. */
    QC_PASSED,
    /** Inspection failed; needs rework. */
    QC_FAILED,
    /** Approved and ready to hand over. */
    READY_FOR_DELIVERY,
    /** Handed to the customer. */
    DELIVERED,
    /** Cancelled. */
    CANCELLED;

    private static final Map<LabJobStatus, Set<LabJobStatus>> ALLOWED = Map.ofEntries(
            Map.entry(WAITING_PROCESS, EnumSet.of(RECEIVED, CANCELLED)),
            Map.entry(RECEIVED, EnumSet.of(IN_PROGRESS, CANCELLED)),
            Map.entry(IN_PROGRESS, EnumSet.of(QC_CHECK, CANCELLED)),
            Map.entry(QC_CHECK, EnumSet.of(QC_PASSED, QC_FAILED)),
            // A failed job may be reworked (BR-33) or abandoned.
            Map.entry(QC_FAILED, EnumSet.of(IN_PROGRESS, CANCELLED)),
            Map.entry(QC_PASSED, EnumSet.of(READY_FOR_DELIVERY)),
            Map.entry(READY_FOR_DELIVERY, EnumSet.of(DELIVERED)),
            Map.entry(DELIVERED, EnumSet.noneOf(LabJobStatus.class)),
            Map.entry(CANCELLED, EnumSet.noneOf(LabJobStatus.class))
    );

    public boolean canTransitionTo(LabJobStatus target) {
        return target != null && ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    /** QC failure must be justified (BR-32). */
    public boolean requiresReasonToEnter() {
        return this == QC_FAILED || this == CANCELLED;
    }

    public static LabJobStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Trạng thái gia công không được để trống");
        }
        String normalized = raw.trim().toUpperCase();
        if ("WAITING".equals(normalized) || "CHO_TIEP_NHAN".equals(normalized)) {
            return WAITING_PROCESS;
        }
        if ("READY_FOR_HANDOVER".equals(normalized)) {
            return READY_FOR_DELIVERY;
        }
        if ("COMPLETED".equals(normalized) || "HANDED_OVER".equals(normalized)) {
            return DELIVERED;
        }
        if ("REWORK".equals(normalized) || "QC_REWORK".equals(normalized)) {
            return QC_FAILED;
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Trạng thái gia công không hợp lệ: " + raw));
    }
}