package com.glassstore.catalog.service;

import com.glassstore.catalog.dto.ProductResponseDTO;
import com.glassstore.catalog.entity.ProductType;

import java.util.List;

public interface ProductService {
    List<ProductResponseDTO> getAllProducts(Long categoryId, ProductType productType, String status);
    ProductResponseDTO getProductById(Long id);
    ProductResponseDTO getProductBySlug(String slug);
}
