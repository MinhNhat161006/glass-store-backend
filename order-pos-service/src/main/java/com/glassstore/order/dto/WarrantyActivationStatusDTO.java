package com.glassstore.order.dto;

import java.time.LocalDateTime;

public record WarrantyActivationStatusDTO(
        Long orderItemId,
        String benefitType,
        String status,
        Integer attempts,
        String lastError,
        LocalDateTime lastAttemptAt,
        LocalDateTime processedAt
) {
}
