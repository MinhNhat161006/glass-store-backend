package com.glassstore.order.controller;

import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
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
import java.util.Map;
import java.util.UUID;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable("id") Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{orderCode}")
    public ResponseEntity<Order> getOrderByCode(@PathVariable("orderCode") String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        if (order.getId() != null || order.getItems() == null || order.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng phải có sản phẩm và không được có ID sẵn.");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getSkuId() == null || item.getSkuId().isBlank()
                    || item.getProductName() == null || item.getProductName().isBlank()
                    || item.getQuantity() == null || item.getQuantity() <= 0
                    || item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU, tên, số lượng và đơn giá sản phẩm không hợp lệ.");
            }
            item.setId(null);
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            total = total.add(item.getSubtotal());
        }
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        if (discount.signum() < 0 || discount.compareTo(total) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giảm giá không hợp lệ.");
        }
        if ("POS".equalsIgnoreCase(order.getChannel())) {
            order.setChannel("POS");
            if (!List.of("CASH", "VIETQR", "CARD").contains(order.getPaymentMethod())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán POS không hợp lệ.");
            }
            order.setTotalAmount(total);
            order.setDiscountAmount(discount);
            order.setTaxAmount(total.subtract(discount).multiply(new BigDecimal("0.08")).setScale(0, RoundingMode.HALF_UP));
            order.setFinalAmount(total.subtract(discount).add(order.getTaxAmount()));
            order.setStatus("PAID");
        }
        if (order.getCustomerName() == null || order.getCustomerName().isBlank()) order.setCustomerName("Khách vãng lai");
        if (order.getCustomerPhone() == null) order.setCustomerPhone("");
        if (order.getOrderCode() == null || order.getOrderCode().isEmpty()) {
            order.setOrderCode("OPT-" + UUID.randomUUID().toString().toUpperCase());
        }
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
                if (item.getSubtotal() == null && item.getUnitPrice() != null) {
                    item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1)));
                }
            }
        }

        if (order.getFinalAmount() == null && order.getTotalAmount() != null) {
            order.setFinalAmount(order.getTotalAmount().subtract(discount));
        }

        Order saved = orderRepository.save(order);

        // Record payment
        Payment payment = Payment.builder()
                .orderId(saved.getId())
                .paymentMethod(saved.getPaymentMethod() != null ? saved.getPaymentMethod() : "VIETQR")
                .amount(saved.getFinalAmount() != null ? saved.getFinalAmount() : BigDecimal.ZERO)
                .transactionCode("TXN-" + System.currentTimeMillis())
                .status("SUCCESS")
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        log.info("Tạo đơn hàng POS thành công: Mã {}", saved.getOrderCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        return orderRepository.findById(id).map(o -> {
            if (body.containsKey("status")) o.setStatus(body.get("status"));
            o.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(orderRepository.save(o));
        }).orElse(ResponseEntity.notFound().build());
    }
}
