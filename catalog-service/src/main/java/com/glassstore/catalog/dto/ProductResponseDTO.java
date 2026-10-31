package com.glassstore.catalog.dto;

import com.glassstore.catalog.entity.ProductType;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String slug;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private ProductType productType;
    private String description;
    private String status;
    private List<ProductVariantDTO> variants;
}
