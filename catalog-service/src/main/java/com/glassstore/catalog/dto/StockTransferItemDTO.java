package com.glassstore.catalog.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockTransferItemDTO {
    private String sku;
    private String productName;
    private Integer quantity;
}
