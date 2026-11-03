package com.glassstore.catalog.controller;

import com.glassstore.catalog.dto.StockDtos;
import com.glassstore.catalog.dto.VariantInfoDTO;
import com.glassstore.catalog.service.InventoryQueryService;
import com.glassstore.catalog.service.StockService;
import com.glassstore.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal endpoints consumed by order-pos-service over Feign.
 *
 * <p>These are not exposed to the public: the gateway does not route {@code /api/variants},
 * and they exist so stock mutations happen inside catalog-service where the data lives.</p>
 */
@RestController
@RequestMapping("/api/variants")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryQueryService queryService;
    private final StockService stockService;

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<VariantInfoDTO>> getBySku(@PathVariable("sku") String sku) {
        return ResponseEntity.ok(ApiResponse.success("OK", queryService.getBySku(sku)));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<VariantInfoDTO>> getByBarcode(@PathVariable("barcode") String barcode) {
        return ResponseEntity.ok(ApiResponse.success("OK", queryService.getByBarcode(barcode)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VariantInfoDTO>>> search(
            @RequestParam(name = "q", required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success("OK", queryService.search(keyword)));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<StockDtos.ReservationResult>> reserve(
            @Valid @RequestBody StockDtos.ReserveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Giữ tồn kho thành công", stockService.reserve(request)));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<StockDtos.ReservationResult>> release(
            @Valid @RequestBody StockDtos.ReleaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Hủy giữ tồn kho thành công", stockService.release(request)));
    }

    @PostMapping("/commit")
    public ResponseEntity<ApiResponse<StockDtos.ReservationResult>> commit(
            @Valid @RequestBody StockDtos.CommitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Xuất kho thành công", stockService.commit(request)));
    }

    /** Manual stock correction. Requires a reason (BR-12). */
    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<Void>> adjust(@Valid @RequestBody StockDtos.AdjustRequest request) {
        stockService.adjust(request.getSku(), request.getDelta(), request.getActor(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho thành công", null));
    }
}