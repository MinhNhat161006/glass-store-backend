package com.glassstore.lab.dto;

import java.time.LocalDateTime;

public record WarrantyOrderItemDTO(
        Long orderId,
        Long orderItemId,
        String orderCode,
        Long customerId,
        String customerName,
        String customerPhone,
        String productSku,
        String productName,
        LocalDateTime purchaseDate,
        String orderStatus,
        String benefitType,
        String activationStatus,
        Integer activationAttempts,
        String activationError
) {
}
