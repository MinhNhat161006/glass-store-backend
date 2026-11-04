package com.glassstore.order.controller;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.order.domain.SalesChannel;
import com.glassstore.order.entity.Voucher;
import com.glassstore.order.repository.VoucherRepository;
import com.glassstore.order.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<Voucher>> get(@PathVariable("code") String code) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> com.glassstore.common.exception.ApiException.of(
                        com.glassstore.common.exception.ErrorCode.VOUCHER_NOT_FOUND,
                        "Mã giảm giá không tồn tại"));
        return ResponseEntity.ok(ApiResponse.success("OK", voucher));
    }

    @GetMapping("/{code}/quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quote(
            @PathVariable("code") String code,
            @RequestParam("total") BigDecimal total,
            @RequestParam(name = "channel", required = false) String channel) {
        SalesChannel salesChannel = SalesChannel.parse(channel);
        BigDecimal discount = voucherService.calculateDiscount(code, total, salesChannel);
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of(
                "code", code,
                "discountAmount", discount,
                "finalAmount", total.subtract(discount).max(BigDecimal.ZERO)
        )));
    }
}
