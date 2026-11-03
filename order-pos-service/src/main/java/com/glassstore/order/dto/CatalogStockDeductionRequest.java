package com.glassstore.order.dto;

import java.util.List;

public record CatalogStockDeductionRequest(
        Long orderId,
        String idempotencyKey,
        String transactionCode,
        String branchName,
        List<Item> items
) {
    public record Item(String sku, Integer quantity) {
    }
}
