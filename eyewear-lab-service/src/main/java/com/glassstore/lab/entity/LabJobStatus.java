package com.glassstore.lab.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum LabJobStatus {
    PENDING("Chờ mài"),
    IN_PROGRESS("Đang mài máy"),
    QC_PENDING("Chờ kiểm định QC"),
    QC_PASSED("Đạt chuẩn QC"),
    QC_FAILED("Lỗi hỏng");

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
        for (LabJobStatus status : LabJobStatus.values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái gia công không hợp lệ: " + value);
    }
}

