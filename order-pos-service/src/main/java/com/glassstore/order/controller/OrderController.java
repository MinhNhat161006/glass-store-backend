package com.glassstore.order.controller;

import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.service.StockDeductionService;
import com.glassstore.order.service.SalesBranch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderController {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleBusinessError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason() == null ? "Yêu cầu không hợp lệ." : error.getReason()));
    }

    private final com.glassstore.order.service.OrderService orderService;
    private final com.glassstore.order.service.OrderCancellationService cancellations;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StockDeductionService stockDeductionService;
    private final com.glassstore.order.client.CatalogInventoryClient catalogInventoryClient;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable("id") Long id) {
        return orderRepository.findById(id)
                .map(order -> { order.setInventorySyncStatus(stockDeductionService.statusForOrder(order.getId())); return ResponseEntity.ok(order); })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{orderCode}")
    public ResponseEntity<Order> getOrderByCode(@PathVariable("orderCode") String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order,
            @RequestHeader(name="Idempotency-Key",required=false) String key,
            @RequestHeader(name="Authorization",required=false) String token) {
        return orderService.create(order,key,token);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable("id") Long id,@RequestBody com.glassstore.order.service.OrderCancellationService.Request request,
            @RequestHeader(name="Authorization",required=false) String token) {
        return ResponseEntity.ok(cancellations.cancel(id,request,token));
    }

    @PostMapping("/{id}/refund-confirm")
    public ResponseEntity<Order> confirmRefund(@PathVariable("id") Long id,@RequestHeader(name="Authorization",required=false) String token) {
        return ResponseEntity.ok(cancellations.confirmRefund(id,token));
    }

    @PostMapping("/{id}/inventory/retry")
    public ResponseEntity<Order> retryInventory(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(stockDeductionService.retry(id, body == null ? null : body.get("branchCode")));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        return orderRepository.findById(id).map(o -> {
            if (body.containsKey("status") && Set.of("CANCELLED","CANCEL_PENDING").contains(body.get("status"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hãy sử dụng API hủy đơn để hoàn kho và quyền lợi.");
            if (Set.of("CANCELLED","CANCEL_PENDING").contains(o.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Không được đổi trạng thái đơn đã hủy.");
            if (body.containsKey("status")) o.setStatus(body.get("status"));
            o.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(orderRepository.save(o));
        }).orElse(ResponseEntity.notFound().build());
    }
}
