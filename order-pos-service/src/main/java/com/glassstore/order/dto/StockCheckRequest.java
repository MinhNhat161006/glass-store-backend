package com.glassstore.order.dto;
import java.util.List;
public record StockCheckRequest(String branchCode, List<CatalogStockDeductionRequest.Item> items) {}
