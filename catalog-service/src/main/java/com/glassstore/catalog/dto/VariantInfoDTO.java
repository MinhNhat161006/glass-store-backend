package com.glassstore.catalog.dto;

import lombok.*;

import java.math.BigDecimal;

/** Read model returned to order-pos-service when it validates a line. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantInfoDTO {
    private String sku;
    private String barcode;
    private String name;
    private BigDecimal price;
    /** Variant status: ACTIVE / INACTIVE / DISCONTINUED. */
    private String status;
    /** Product-level status, so BR-04 can be enforced on discontinuation. */
    private String productStatus;
    /** Sellable units right now (BR-08). */
    private Integer availableQuantity;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer nonSellableQuantity;
    private String imageUrl;
}