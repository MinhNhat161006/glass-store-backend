package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.LabClient;
import com.glassstore.order.domain.*;
import com.glassstore.order.dto.OrderDtos;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Checkout and settlement.
 *
 * <p>The client may only choose what to buy, how to pay, and contact details.
 * Prices and totals are recomputed server-side (BR-05, BR-16).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCheckoutService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BasketPricingService pricingService;
    private final OrderStatusService orderStatusService;
    private final OrderInventoryService inventoryService;
    private final LabClient labClient;

    @Transactional
    public CheckoutOutcome checkout(OrderDtos.CheckoutRequest request, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);

        Optional<Order> existing = orderRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            Order order = existing.get();
            log.info("Checkout replayed for idempotencyKey={} -> order {}", key, order.getOrderCode());
            return new CheckoutOutcome(order, true);
        }

        PaymentMethod method = PaymentMethod.parse(request.getPaymentMethod());
        SalesChannel channel = SalesChannel.parse(request.getChannel());
        OrderType orderType = OrderType.parse(request.getOrderType());

        OrderDtos.PricedBasket basket = pricingService.price(request.getItems(), request.getVoucherCode(), channel);

        if (basket.getPriceChanges() != null && !basket.getPriceChanges().isEmpty()
                && !request.isAcceptPriceChange()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("priceChanges", basket.getPriceChanges());
            details.put("newTotal", basket.getFinalAmount());
            throw new ApiException(ErrorCode.ORDER_PRICE_CHANGED,
                    "Giá sản phẩm đã thay đổi. Vui lòng xác nhận giá mới trước khi đặt hàng.", details);
        }

        if (orderType.requiresPrescription() && request.getPrescriptionId() == null) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Đơn kính thuốc bắt buộc phải có toa kính đã được xác nhận");
        }

        if (request.getCustomerName() == null || request.getCustomerName().isBlank()
                || request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Thiếu thông tin khách hàng");
        }

        String orderCode = nextOrderCode();
        Order order = Order.builder()
                .orderCode(orderCode)
                .idempotencyKey(key)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .shippingAddress(request.getShippingAddress())
                .channel(channel)
                .orderType(orderType)
                .totalAmount(basket.getTotalAmount())
                .discountAmount(basket.getDiscountAmount())
                .finalAmount(basket.getFinalAmount())
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentMethod(method)
                .voucherCode(request.getVoucherCode())
                .prescriptionId(request.getPrescriptionId())
                .staffName(request.getStaffName())
                .branchName(request.getBranchName())
                .notes(request.getNotes())
                .posSessionId(request.getPosSessionId())
                .invoiceNumber(channel == SalesChannel.POS ? "INV-" + orderCode : null)
                .build();

        for (OrderDtos.OrderItemSnapshot line : basket.getLines()) {
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .skuId(line.getSku())
                    .productName(line.getProductName())
                    .quantity(line.getQuantity())
                    .unitPrice(line.getUnitPrice())
                    .subtotal(line.getSubtotal())
                    .prescriptionId(request.getPrescriptionId())
                    .build());
        }

        Order saved = orderRepository.save(order);

        Payment payment = Payment.builder()
                .orderId(saved.getId())
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .amount(saved.getFinalAmount())
                .provider(method.requiresGateway() ? "UNCONFIGURED" : null)
                .transactionCode("TXN-" + saved.getOrderCode())
                .build();
        paymentRepository.save(payment);

        try {
            inventoryService.reserve(saved);
        } catch (Exception ex) {
            log.error("Stock reservation failed for order {}: {}", saved.getOrderCode(), ex.getMessage());
            orderRepository.delete(saved);
            throw ex;
        }

        log.info("Order {} created: {} line(s), final={}, method={}, channel={}",
                saved.getOrderCode(), saved.getItems().size(), saved.getFinalAmount(), method, channel);

        return new CheckoutOutcome(saved, false);
    }

    @Transactional
    public Order settleCash(Long orderId, String actor) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.of(ErrorCode.ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng ID: " + orderId));

        if (order.getPaymentMethod() != PaymentMethod.CASH) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "Chỉ xác nhận tiền mặt cho đơn hàng có phương thức CASH");
        }
        return markPaid(order, actor, "Thu tiền mặt tại quầy", "CASH");
    }

    /**
     * Marks an order paid after a verified payment source (cash staff action or gateway).
     */
    @Transactional
    public Order markPaid(Order order, String actor, String reason, String provider) {
        if (paymentRepository.existsByOrderIdAndStatus(order.getId(), PaymentStatus.SUCCESS)) {
            throw ApiException.of(ErrorCode.PAYMENT_ALREADY_SETTLED,
                    "Đơn hàng " + order.getOrderCode() + " đã được thanh toán");
        }

        Payment payment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> ApiException.of(ErrorCode.PAYMENT_NOT_FOUND,
                        "Không có giao dịch chờ thanh toán cho đơn " + order.getOrderCode()));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        if (provider != null) {
            payment.setProvider(provider);
        }
        if (payment.getTransactionCode() == null) {
            payment.setTransactionCode(provider + "-" + order.getOrderCode());
        }
        paymentRepository.save(payment);

        OrderStatus previous = order.currentStatus();
        Order paid = orderStatusService.transition(order, OrderStatus.PAID, actor, reason);
        inventoryService.onStatusChanged(paid, previous, OrderStatus.PAID);
        dispatchLabJobIfNeeded(paid);
        return paid;
    }

    public void dispatchLabJobIfNeeded(Order order) {
        if (order.getPrescriptionId() == null || order.getOrderType() == null
                || !order.getOrderType().requiresPrescription()) {
            return;
        }
        try {
            String frameSku = null;
            String frameName = null;
            String lensSku = null;
            String lensName = null;
            for (OrderItem item : order.getItems()) {
                String sku = item.getSkuId() == null ? "" : item.getSkuId().toUpperCase();
                if (sku.startsWith("LNS-") || sku.startsWith("LZ-")) {
                    lensSku = item.getSkuId();
                    lensName = item.getProductName();
                } else {
                    frameSku = item.getSkuId();
                    frameName = item.getProductName();
                }
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("orderId", order.getId());
            body.put("prescriptionId", order.getPrescriptionId());
            body.put("frameSku", frameSku);
            body.put("frameName", frameName);
            body.put("lensSku", lensSku);
            body.put("lensName", lensName);
            body.put("technicianName", order.getStaffName());
            labClient.createFromOrder(body);
            log.info("Dispatched lab job for order {}", order.getOrderCode());
        } catch (Exception ex) {
            log.error("Could not create lab job for order {}: {}", order.getOrderCode(), ex.getMessage());
        }
    }

    private String requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw ApiException.of(ErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                    "Thiếu header Idempotency-Key cho yêu cầu tạo đơn");
        }
        return key.trim();
    }

    private String nextOrderCode() {
        String datePart = java.time.LocalDate.now().format(CODE_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "ORD-" + datePart + "-"
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (orderRepository.findByOrderCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw ApiException.of(ErrorCode.CONFLICT, "Không sinh được mã đơn hàng duy nhất");
    }

    public record CheckoutOutcome(Order order, boolean replayed) {
    }

    static boolean totalsConsistent(Order order) {
        BigDecimal sum = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BasketPricingService.ZERO, BigDecimal::add);
        return sum.compareTo(order.getTotalAmount()) == 0;
    }
}
