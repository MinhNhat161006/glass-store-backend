package com.glassstore.order.controller;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.order.dto.WarrantyActivationStatusDTO;
import com.glassstore.order.service.WarrantyActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/warranty-activations")
@RequiredArgsConstructor
public class WarrantyActivationController {

    private final WarrantyActivationService activationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarrantyActivationStatusDTO>>> getStatuses(
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Trạng thái kích hoạt bảo hành", activationService.statusesForOrder(orderId)));
    }

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<List<WarrantyActivationStatusDTO>>> retry(
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Đã đưa các kích hoạt lỗi vào hàng đợi thử lại",
                activationService.retryForOrder(orderId)));
    }
}
