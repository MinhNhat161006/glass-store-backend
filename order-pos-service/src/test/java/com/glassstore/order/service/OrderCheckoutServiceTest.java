package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.LabClient;
import com.glassstore.order.domain.OrderStatus;
import com.glassstore.order.domain.PaymentMethod;
import com.glassstore.order.domain.PaymentStatus;
import com.glassstore.order.dto.OrderDtos;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderCheckoutServiceTest {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private BasketPricingService pricingService;
    private OrderStatusService orderStatusService;
    private OrderInventoryService inventoryService;
    private LabClient labClient;
    private OrderCheckoutService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        pricingService = mock(BasketPricingService.class);
        orderStatusService = mock(OrderStatusService.class);
        inventoryService = mock(OrderInventoryService.class);
        labClient = mock(LabClient.class);
        service = new OrderCheckoutService(orderRepository, paymentRepository, pricingService,
                orderStatusService, inventoryService, labClient);

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
        when(pricingService.price(any(), any(), any())).thenReturn(basket());
    }

    @Test
    @DisplayName("Idempotent checkout returns the original order (EX-ORD-04)")
    void replay() {
        Order existing = Order.builder().id(9L).orderCode("ORD-1").build();
        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        OrderCheckoutService.CheckoutOutcome outcome = service.checkout(validRequest(), "key-1");

        assertThat(outcome.replayed()).isTrue();
        assertThat(outcome.order().getId()).isEqualTo(9L);
        verify(inventoryService, never()).reserve(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Missing Idempotency-Key is rejected")
    void missingKey() {
        assertThatThrownBy(() -> service.checkout(validRequest(), " "))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    @DisplayName("Price change without accept flag is rejected (EX-ORD-02)")
    void priceChanged() {
        when(pricingService.price(any(), any(), any())).thenReturn(OrderDtos.PricedBasket.builder()
                .lines(basket().getLines())
                .totalAmount(new BigDecimal("20.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("20.00"))
                .priceChanges(List.of(OrderDtos.PriceChange.builder()
                        .sku("SKU-1")
                        .expectedUnitPrice(new BigDecimal("10.00"))
                        .currentUnitPrice(new BigDecimal("20.00"))
                        .build()))
                .build());

        assertThatThrownBy(() -> service.checkout(validRequest(), "k"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_PRICE_CHANGED);
        verify(inventoryService, never()).reserve(any());
    }

    @Test
    @DisplayName("Successful checkout snapshots catalog prices and reserves stock")
    void success() {
        OrderCheckoutService.CheckoutOutcome outcome = service.checkout(validRequest(), "k-ok");

        assertThat(outcome.replayed()).isFalse();
        assertThat(outcome.order().getFinalAmount()).isEqualByComparingTo("10.00");
        assertThat(outcome.order().getItems()).hasSize(1);
        assertThat(outcome.order().getItems().get(0).getUnitPrice()).isEqualByComparingTo("10.00");
        verify(inventoryService).reserve(outcome.order());
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Lens order without prescription is rejected")
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
