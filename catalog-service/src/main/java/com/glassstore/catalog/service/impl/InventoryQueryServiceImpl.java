package com.glassstore.catalog.service.impl;

import com.glassstore.catalog.dto.VariantInfoDTO;
import com.glassstore.catalog.entity.ProductVariant;
import com.glassstore.catalog.repository.ProductVariantRepository;
import com.glassstore.catalog.service.InventoryQueryService;
import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {

    private final ProductVariantRepository variantRepository;

    @Override
    public VariantInfoDTO getBySku(String sku) {
        return variantRepository.findBySku(normalize(sku))
                .map(this::toDto)
                .orElseThrow(() -> ApiException.of(ErrorCode.VARIANT_NOT_FOUND,
                        "Không tìm thấy SKU: " + sku));
    }

    @Override
    public VariantInfoDTO getByBarcode(String barcode) {
        return variantRepository.findByBarcode(normalize(barcode))
                .map(this::toDto)
                .orElseThrow(() -> ApiException.of(ErrorCode.VARIANT_NOT_FOUND,
                        "Không tìm thấy mã vạch: " + barcode));
    }

    @Override
    public List<VariantInfoDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String needle = keyword.trim().toLowerCase();
        // Small dataset (15 SKUs); an in-memory filter keeps this readable and avoids
        // adding a custom query for every search combination.
        return variantRepository.findAll().stream()
                .filter(v -> matches(v, needle))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private boolean matches(ProductVariant v, String needle) {
        return (v.getSku() != null && v.getSku().toLowerCase().contains(needle))
                || (v.getBarcode() != null && v.getBarcode().contains(needle))
                || (v.getName() != null && v.getName().toLowerCase().contains(needle));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private VariantInfoDTO toDto(ProductVariant v) {
        return VariantInfoDTO.builder()
                .sku(v.getSku())
                .barcode(v.getBarcode())
                .name(v.getName())
                .price(v.getPrice())
                .status(v.getStatus())
                .productStatus(v.getProduct() != null ? v.getProduct().getStatus() : null)
                .availableQuantity(v.getAvailableQuantity())
                .stockQuantity(v.getStockQuantity())
                .reservedQuantity(v.getReservedQuantity())
                .nonSellableQuantity(v.getNonSellableQuantity())
                .imageUrl(v.getImageUrl())
                .build();
    }
}