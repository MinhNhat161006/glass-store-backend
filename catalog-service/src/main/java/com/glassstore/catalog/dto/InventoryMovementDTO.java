package com.glassstore.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryMovementDTO {
    private Long id;
    private String movementType;
    private String sku;
    private String sourceLocationCode;
    private String destinationLocationCode;
    private String referenceType;
    private String referenceId;
    private Integer quantity;
    private String performedBy;
    private LocalDateTime createdAt;
}
