package com.glassstore.lab.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum LabJobStatus {
    PENDING("Chờ mài"),
    WAITING_PROCESS("Chờ tiếp nhận"),
    RECEIVED("Đã tiếp nhận"),
    IN_PROGRESS("Đang mài máy"),
    QC_PENDING("Chờ kiểm định QC"),
    QC_CHECK("Đang kiểm định QC"),
    QC_PASSED("Đạt chuẩn QC"),
    QC_FAILED("Lỗi hỏng"),
    READY_FOR_DELIVERY("Sẵn sàng bàn giao"),
    DELIVERED("Đã bàn giao"),
    CANCELLED("Đã hủy");

    private final String description;

    LabJobStatus(String description) {
        this.description = description;
    }

    @JsonCreator
    public static LabJobStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String clean = value.trim().toUpperCase();
        if ("COMPLETED".equals(clean)) {
            return QC_PASSED;
        }
        if ("WAITING".equals(clean) || "CHO_TIEP_NHAN".equals(clean)) {
            return WAITING_PROCESS;
        }
        if ("READY_FOR_HANDOVER".equals(clean)) {
            return READY_FOR_DELIVERY;
        }
        if ("HANDED_OVER".equals(clean)) {
            return DELIVERED;
        }
        if ("REWORK".equals(clean) || "QC_REWORK".equals(clean)) {
            return QC_FAILED;
        }
        for (LabJobStatus status : LabJobStatus.values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái gia công không hợp lệ: " + value);
    }
}
