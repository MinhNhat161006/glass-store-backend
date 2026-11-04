package com.glassstore.catalog.dto;

public record StockDeductionResponse(Long orderId, String locationCode, boolean duplicate) {
}
