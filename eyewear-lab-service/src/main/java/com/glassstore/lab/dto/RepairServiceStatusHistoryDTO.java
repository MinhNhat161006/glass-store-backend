package com.glassstore.lab.dto;

import java.time.LocalDateTime;

public record RepairServiceStatusHistoryDTO(
        String previousStatus,
        String newStatus,
        String changedBy,
        LocalDateTime changedAt,
        String notes
) {
}
