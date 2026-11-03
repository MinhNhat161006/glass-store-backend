package com.glassstore.order.dto;

public record CatalogStockDeductionResponse(Long orderId, String locationCode, boolean duplicate) {
}
