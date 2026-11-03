package com.glassstore.order.client;

import com.glassstore.order.dto.CatalogStockDeductionRequest;
import com.glassstore.order.dto.CatalogStockDeductionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "catalog-inventory",
        url = "${services.catalog-service.url:http://localhost:8082}"
)
public interface CatalogInventoryClient {

    @PostMapping("/internal/inventory/sales/deduct")
    CatalogStockDeductionResponse deductPaidOrder(@RequestBody CatalogStockDeductionRequest request);
}
