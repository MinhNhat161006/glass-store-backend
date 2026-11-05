package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.LabClient;
import com.glassstore.order.client.LabServiceClient;
import com.glassstore.order.domain.*;
import com.glassstore.order.dto.CreateLabJobRequest;
import com.glassstore.order.dto.OrderDtos;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class OrderCheckoutService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BasketPricingService pricingService;
    private final OrderStatusService orderStatusService;
    private final OrderInventoryService inventoryService;
    private final LabClient labClient;
    private final WarrantyActivationService warrantyActivationService;
    private final LabServiceClient labServiceClient;

    @Autowired
    public OrderCheckoutService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            BasketPricingService pricingService,
            OrderStatusService orderStatusService,
            OrderInventoryService inventoryService,
            @Autowired(required = false) LabClient labClient,
            WarrantyActivationService warrantyActivationService,
            @Autowired(required = false) LabServiceClient labServiceClient) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
        this.orderStatusService = orderStatusService;
        this.inventoryService = inventoryService;
        this.labClient = labClient;
        this.warrantyActivationService = warrantyActivationService;
        this.labServiceClient = labServiceClient;
    }

    public OrderCheckoutService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            BasketPricingService pricingService,
            OrderStatusService orderStatusService,
            OrderInventoryService inventoryService,
            LabClient labClient,
            WarrantyActivationService warrantyActivationService) {
        this(orderRepository, paymentRepository, pricingService, orderStatusService,
                inventoryService, labClient, warrantyActivationService, null);
    }

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

        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Thiếu tên khách hàng");
        }
        if (channel != SalesChannel.POS
                && (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank())) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Thiếu số điện thoại khách hàng");
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            request.setCustomerPhone("0000000000");
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
        warrantyActivationService.enqueueForPaidOrder(paid);
        dispatchLabJobIfNeeded(paid);
        return paid;
    }

    /**
     * Tự động sinh lệnh mài Lens (lab_job) qua OpenFeign khi đơn hàng có kính thuốc / toa đo mắt.
     */
    public void dispatchLabJobIfNeeded(Order order) {
        if (order == null) {
            return;
        }

        // 1. Kiểm tra nếu đơn hàng có sản phẩm là kính thuốc (hoặc có gắn toa đo mắt prescription)
        boolean hasPrescription = order.getPrescriptionId() != null;
        boolean isPrescriptionOrder = order.getOrderType() != null && order.getOrderType().requiresPrescription();
        boolean hasPrescriptionItem = order.getItems() != null && order.getItems().stream().anyMatch(i ->
                i.getPrescriptionId() != null || isLensSku(i.getSkuId(), i.getProductName()));

        if (!hasPrescription && !isPrescriptionOrder && !hasPrescriptionItem) {
            log.debug("Đơn hàng {} không có sản phẩm kính thuốc hoặc toa kính, bỏ qua tạo lệnh mài", order.getOrderCode());
            return;
        }

        try {
            String frameSku = null;
            String frameName = null;
            String lensSku = null;
            String lensName = null;
            Long itemPrescriptionId = null;

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (isLensSku(item.getSkuId(), item.getProductName())) {
                        lensSku = item.getSkuId();
                        lensName = item.getProductName();
                        if (item.getPrescriptionId() != null) {
                            itemPrescriptionId = item.getPrescriptionId();
                        }
                    } else {
                        frameSku = item.getSkuId();
                        frameName = item.getProductName();
                    }
                }
            }

            Long effectivePrescriptionId = order.getPrescriptionId() != null
                    ? order.getPrescriptionId()
                    : itemPrescriptionId;

            // 2. Truy vấn thông số đo mắt từ Prescription nếu có
            CreateLabJobRequest.PrescriptionDto prescriptionDto = null;
            if (effectivePrescriptionId != null && labServiceClient != null) {
                try {
                    var rxRes = labServiceClient.getPrescriptionById(effectivePrescriptionId);
                    if (rxRes != null && rxRes.getBody() != null) {
                        Map<String, Object> body = rxRes.getBody();
                        Object dataObj = body.get("data");
                        if (dataObj instanceof Map<?, ?> dataMap) {
                            prescriptionDto = parsePrescriptionDto(dataMap, effectivePrescriptionId);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Không thể tải chi tiết toa kính ID {} từ lab-service: {}",
                            effectivePrescriptionId, ex.getMessage());
                }
            }

            // 3. Tự động build DTO CreateLabJobRequest
            String trayNumber = "KHAY-" + (order.getOrderCode() != null ? order.getOrderCode() : order.getId());

            CreateLabJobRequest labJobRequest = CreateLabJobRequest.builder()
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .trayNumber(trayNumber)
                    .customerName(order.getCustomerName())
                    .prescriptionId(effectivePrescriptionId)
                    .frameSku(frameSku)
                    .frameName(frameName)
                    .lensSku(lensSku)
                    .lensName(lensName)
                    .technicianName(order.getStaffName())
                    .prescription(prescriptionDto)
                    .build();

            // 4. Gọi LabServiceClient.createLabJob(...)
            if (labServiceClient != null) {
                labServiceClient.createLabJob(labJobRequest);
                log.info("Tự động sinh lệnh mài lens (Lab Job) thành công cho đơn POS: mã {}, khay {}",
                        order.getOrderCode(), trayNumber);
            } else if (labClient != null) {
                // Fallback nếu LabServiceClient chưa được tiêm
                Map<String, Object> fallbackBody = new LinkedHashMap<>();
                fallbackBody.put("orderId", order.getId());
                fallbackBody.put("prescriptionId", effectivePrescriptionId);
                fallbackBody.put("frameSku", frameSku);
                fallbackBody.put("frameName", frameName);
                fallbackBody.put("lensSku", lensSku);
                fallbackBody.put("lensName", lensName);
                fallbackBody.put("technicianName", order.getStaffName());
                labClient.createFromOrder(fallbackBody);
                log.info("Dispatched lab job via fallback LabClient for order {}", order.getOrderCode());
            }
        } catch (Exception ex) {
            // Bọc trong try-catch để nếu lab-service gặp sự cố thì KHÔNG làm rollback transaction thanh toán POS
            log.warn("Cảnh báo: Không thể tự động tạo lệnh mài kính cho đơn hàng {} do lỗi lab-service: {}",
                    order.getOrderCode(), ex.getMessage());
        }
    }

    private boolean isLensSku(String sku, String productName) {
        String cleanSku = sku == null ? "" : sku.toUpperCase();
        if (cleanSku.startsWith("LNS-") || cleanSku.startsWith("LZ-") || cleanSku.contains("LENS")) {
            return true;
        }
        return productName != null && productName.toLowerCase().contains("tròng");
    }

    private CreateLabJobRequest.PrescriptionDto parsePrescriptionDto(Map<?, ?> map, Long rxId) {
        return CreateLabJobRequest.PrescriptionDto.builder()
                .id(rxId)
                .odSphere(asBigDecimal(map.get("odSph") != null ? map.get("odSph") : map.get("odSphere")))
                .odCyl(asBigDecimal(map.get("odCyl")))
                .odAxis(asInteger(map.get("odAxis")))
                .odAdd(asBigDecimal(map.get("odAdd")))
                .osSphere(asBigDecimal(map.get("osSph") != null ? map.get("osSph") : map.get("osSphere")))
                .osCyl(asBigDecimal(map.get("osCyl")))
                .osAxis(asInteger(map.get("osAxis")))
                .osAdd(asBigDecimal(map.get("osAdd")))
                .pd(asBigDecimal(map.get("pd")))
                .notes(map.get("notes") != null ? map.get("notes").toString() : null)
                .build();
    }

    private BigDecimal asBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer asInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number num) return num.intValue();
        try {
            return Integer.valueOf(obj.toString());
        } catch (Exception e) {
            return null;
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
