package com.glassstore.order.controller;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.order.domain.OrderStatus;
import com.glassstore.order.dto.OrderDtos;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderStatusHistory;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.OrderStatusHistoryRepository;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.service.OrderCheckoutService;
import com.glassstore.order.service.OrderStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order API for both the storefront and the internal portal.
 *
 * <p>{@code GET /api/orders} intentionally returns a bare array (not an envelope) because the
 * existing portal does {@code Array.isArray(...)} on the response. Every other endpoint uses
 * the standard {@link ApiResponse} shape.</p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderCheckoutService checkoutService;
    private final OrderStatusService orderStatusService;

    /** Bare array to keep the existing portal contract working. */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<Order>>> getByCustomer(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Customer-Id", required = false) Long customerId) {
        Order order = requireOrder(id);
        assertOwner(order, customerId);
        return ResponseEntity.ok(ApiResponse.success("OK", order));
    }

    @GetMapping("/code/{orderCode}")
    public ResponseEntity<ApiResponse<Order>> getOrderByCode(@PathVariable("orderCode") String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> com.glassstore.common.exception.ApiException.of(
                        com.glassstore.common.exception.ErrorCode.ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng: " + orderCode));
        return ResponseEntity.ok(ApiResponse.success("OK", order));
    }

    /** Payment history for an order (FR-PAY-08). */
    @GetMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<List<Payment>>> getPayments(@PathVariable("id") Long id) {
        requireOrder(id);
        return ResponseEntity.ok(ApiResponse.success("OK",
                paymentRepository.findByOrderIdOrderByCreatedAtDesc(id)));
    }

    /** Audit trail of status changes (SRS §4.6). */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistory>>> getHistory(@PathVariable("id") Long id) {
        requireOrder(id);
        return ResponseEntity.ok(ApiResponse.success("OK",
                historyRepository.findByOrderIdOrderByChangedAtAsc(id)));
    }

    /**
     * Checkout. Requires {@code Idempotency-Key}; a replay returns 200 with the original
     * order, a first submission returns 201.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderDtos.CheckoutRequest request) {

        OrderCheckoutService.CheckoutOutcome outcome =
                checkoutService.checkout(request, idempotencyKey);

        HttpStatus status = outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        String message = outcome.replayed()
                ? "Đơn hàng đã được tạo trước đó (yêu cầu được gửi lại)"
                : "Tạo đơn hàng thành công";
        return ResponseEntity.status(status).body(ApiResponse.success(message, outcome.order()));
    }

    /** Staff confirms cash received at the counter (FR-PAY-04). */
    @PostMapping("/{id}/settle-cash")
    public ResponseEntity<ApiResponse<Order>> settleCash(
            @PathVariable("id") Long id,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán tiền mặt thành công",
                checkoutService.settleCash(id, actor)));
    }

    /** Validated status change (SRS §4.6). */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody OrderDtos.StatusUpdateRequest body) {

        Order order = requireOrder(id);
        OrderStatus target;
        try {
            target = OrderStatus.parse(body.getStatus());
        } catch (IllegalArgumentException ex) {
            throw com.glassstore.common.exception.ApiException.of(
                    com.glassstore.common.exception.ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
        Order updated = orderStatusService.transition(order, target, body.getActor(), body.getReason());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", updated));
    }

    private Order requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> com.glassstore.common.exception.ApiException.of(
                        com.glassstore.common.exception.ErrorCode.ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng ID: " + id));
    }

    private void assertOwner(Order order, Long customerId) {
        if (customerId == null) {
            return;
        }
        if (order.getCustomerId() == null || !order.getCustomerId().equals(customerId)) {
            throw com.glassstore.common.exception.ApiException.of(
                    com.glassstore.common.exception.ErrorCode.ORDER_FORBIDDEN,
                    "Bạn không có quyền truy cập đơn hàng này");
        }
    }
}