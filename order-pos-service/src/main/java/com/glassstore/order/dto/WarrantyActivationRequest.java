package com.glassstore.order.dto;

public record WarrantyActivationRequest(Long orderId, Long orderItemId, String benefitType) {
}
