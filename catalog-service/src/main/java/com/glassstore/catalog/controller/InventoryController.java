package com.glassstore.catalog.controller;

import com.glassstore.catalog.dto.*;
import com.glassstore.catalog.service.InventoryService;
import com.glassstore.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/api/inventory")
    public ResponseEntity<ApiResponse<List<InventoryRowDTO>>> getInventory() {
        return ResponseEntity.ok(ApiResponse.success("Lấy tồn kho theo địa điểm thành công", inventoryService.getInventory()));
    }

    @GetMapping("/api/inventory/locations")
    public ResponseEntity<ApiResponse<List<InventoryLocationDTO>>> getLocations() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kho thành công", inventoryService.getLocations()));
    }

    @GetMapping("/api/inventory/transfers")
    public ResponseEntity<ApiResponse<List<StockTransferDTO>>> getTransfers() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phiếu điều chuyển thành công", inventoryService.getTransfers()));
    }

    @PostMapping("/api/inventory/transfers")
    public ResponseEntity<ApiResponse<StockTransferDTO>> createTransfer(
            @Valid @RequestBody CreateStockTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phiếu điều chuyển thành công", inventoryService.createTransfer(request)));
    }

    @PatchMapping("/api/inventory/transfers/{id}/dispatch")
    public ResponseEntity<ApiResponse<StockTransferDTO>> dispatchTransfer(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success("Xuất kho điều chuyển thành công", inventoryService.dispatchTransfer(id)));
    }

    @PatchMapping("/api/inventory/transfers/{id}/receive")
    public ResponseEntity<ApiResponse<StockTransferDTO>> receiveTransfer(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận nhận hàng thành công", inventoryService.receiveTransfer(id)));
    }

    @PostMapping("/internal/inventory/sales/deduct")
    public ResponseEntity<StockDeductionResponse> deductPaidOrder(
            @Valid @RequestBody StockDeductionRequest request) {
        return ResponseEntity.ok(inventoryService.deductPaidOrder(request));
    }
}
