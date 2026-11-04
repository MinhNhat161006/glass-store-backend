package com.glassstore.lab.dto;

import com.glassstore.lab.entity.Warranty;

import java.time.LocalDateTime;
import java.util.List;

public record WarrantyLookupItemDTO(
        Long orderId,
        Long orderItemId,
        String orderCode,
        LocalDateTime purchaseDate,
        String customerName,
        String customerPhone,
        String productName,
        String productSku,
        String productBarcode,
        String benefitType,
        String activationStatus,
        Integer activationAttempts,
        String activationError,
        List<Warranty> warranties
) {
}
