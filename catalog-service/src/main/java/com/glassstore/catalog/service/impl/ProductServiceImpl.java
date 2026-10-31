package com.glassstore.catalog.service.impl;

import com.glassstore.catalog.dto.ProductResponseDTO;
import com.glassstore.catalog.dto.ProductVariantDTO;
import com.glassstore.catalog.entity.Product;
import com.glassstore.catalog.entity.ProductType;
import com.glassstore.catalog.repository.ProductRepository;
import com.glassstore.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponseDTO> getAllProducts(Long categoryId, ProductType productType, String status) {
        List<Product> products;
        
        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (productType != null) {
            products = productRepository.findByProductType(productType);
        } else if (status != null) {
            products = productRepository.findByStatus(status);
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToDTO(product);
    }

    @Override
    public ProductResponseDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        return mapToDTO(product);
    }

    private ProductResponseDTO mapToDTO(Product product) {
        List<ProductVariantDTO> variantDTOs = product.getVariants().stream()
                .map(v -> ProductVariantDTO.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .barcode(v.getBarcode())
                        .name(v.getName())
                        .color(v.getColor())
                        .frameShape(v.getFrameShape())
                        .material(v.getMaterial())
                        .lensIndex(v.getLensIndex())
                        .price(v.getPrice())
                        .stockQuantity(v.getStockQuantity())
                        .imageUrl(v.getImageUrl())
                        .status(v.getStatus())
                        .build())
                .collect(Collectors.toList());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .productType(product.getProductType())
                .description(product.getDescription())
                .status(product.getStatus())
                .variants(variantDTOs)
                .build();
    }
}
