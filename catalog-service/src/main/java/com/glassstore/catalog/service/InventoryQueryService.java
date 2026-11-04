package com.glassstore.catalog.service;

import com.glassstore.catalog.dto.VariantInfoDTO;

import java.util.List;

/** Read-side catalog access used by the selling channels. */
public interface InventoryQueryService {

    VariantInfoDTO getBySku(String sku);

    VariantInfoDTO getByBarcode(String barcode);

    List<VariantInfoDTO> search(String keyword);
}