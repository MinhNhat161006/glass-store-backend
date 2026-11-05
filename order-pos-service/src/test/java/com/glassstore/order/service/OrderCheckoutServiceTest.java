package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.domain.OrderStatus;
import com.glassstore.order.domain.OrderType;
import com.glassstore.order.domain.PaymentMethod;
import com.glassstore.order.domain.PaymentStatus;
import com.glassstore.order.dto.OrderDtos;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderCheckoutServiceTest {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private BasketPricingService pricingService;
    private OrderStatusService orderStatusService;
    private OrderInventoryService inventoryService;
    private LabDispatchService labDispatch;
    private WarrantyActivationService warrantyActivationService;
    private OrderCheckoutService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        pricingService = mock(BasketPricingService.class);
        orderStatusService = mock(OrderStatusService.class);
        inventoryService = mock(OrderInventoryService.class);
        labDispatch = mock(LabDispatchService.class);
        warrantyActivationService = mock(WarrantyActivationService.class);
        service = new OrderCheckoutService(orderRepository, paymentRepository, pricingService,
                orderStatusService, inventoryService, labDispatch, warrantyActivationService);

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderRepository.findByOrderCode(any())).thenReturn(Optional.empty());
        AtomicLong ids = new AtomicLong(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(ids.getAndIncrement());
            }
            return order;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pricingService.price(any(), any(), any())).thenReturn(basket());
    }

    @Test
    @DisplayName("Online checkout requires a non-blank phone number (BR-16)")
    void onlinePhoneRequired() {
        OrderDtos.CheckoutRequest request = validRequest();
        request.setCustomerPhone(" ");
        assertThatThrownBy(() -> service.checkout(request, "k"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("Pos checkout falls back to placeholder phone when blank")
    void posPhoneFallback() {
        OrderDtos.CheckoutRequest request = validRequest();
        request.setChannel("POS");
        request.setCustomerPhone("");
        OrderCheckoutService.CheckoutOutcome outcome = service.checkout(request, "k");
        assertThat(outcome.order().getCustomerPhone()).isEqualTo("0000000000");
    }

    @Test
    @DisplayName("Prescription is mandatory for prescription-bearing orders (BR-03)")
    void prescriptionRequired() {
        OrderDtos.CheckoutRequest request = validRequest();
        request.setOrderType("COMPLETE_GLASSES");
        request.setPrescriptionId(null);
        assertThatThrownBy(() -> service.checkout(request, "k"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRESCRIPTION_INCOMPLETE);
    }

    @Test
    @DisplayName("Cash settlement cannot run twice (BR-15)")
    void cashAlreadySettled() {
        Order order = Order.builder().id(1L).orderCode("ORD-X").paymentMethod(PaymentMethod.CASH)
                .status(OrderStatus.PENDING_PAYMENT).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(true);

        assertThatThrownBy(() -> service.settleCash(1L, "STAFF"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_SETTLED);
    }

    @Test
    @DisplayName("dispatchLabJobIfNeeded: Chuyển tiếp đơn hàng sang labDispatch.enqueue")
    void dispatchLabJob_DelegatesToLabDispatch() {
        List<OrderItem> items = new ArrayList<>();
        items.add(OrderItem.builder().skuId("LNS-CRIZAL-160").productName("Tròng Crizal").quantity(1).build());

        Order order = Order.builder()
                .id(101L)
                .orderCode("ORD-20261104-001")
                .customerName("Nguyễn Văn A")
                .orderType(OrderType.COMPLETE_GLASSES)
                .prescriptionId(55L)
                .items(items)
                .build();

        service.dispatchLabJobIfNeeded(order);

        verify(labDispatch, times(1)).enqueue(order);
    }

    private static OrderDtos.CheckoutRequest validRequest() {
        return OrderDtos.CheckoutRequest.builder()
                .customerName("A")
                .customerPhone("090")
                .customerId(5L)
                .items(List.of(OrderDtos.CheckoutLine.builder()
                        .sku("SKU-1").quantity(1).expectedUnitPrice(new BigDecimal("10.00")).build()))
                .paymentMethod("CASH")
                .channel("ONLINE")
                .orderType("ACCESSORY")
                .build();
    }

    private static OrderDtos.PricedBasket basket() {
        return OrderDtos.PricedBasket.builder()
                .lines(List.of(OrderDtos.OrderItemSnapshot.builder()
                        .sku("SKU-1").productName("Item").quantity(1)
                        .unitPrice(new BigDecimal("10.00"))
                        .subtotal(new BigDecimal("10.00"))
                        .build()))
                .totalAmount(new BigDecimal("10.00"))
                .discountAmount(BigDecimal.ZERO.setScale(2))
                .finalAmount(new BigDecimal("10.00"))
                .priceChanges(List.of())
                .build();
    }
}