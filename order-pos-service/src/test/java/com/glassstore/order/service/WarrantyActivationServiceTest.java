package com.glassstore.order.service;

import com.glassstore.order.client.WarrantyClient;
import com.glassstore.order.domain.PaymentStatus;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.WarrantyActivationOutbox;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.repository.WarrantyActivationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarrantyActivationServiceTest {

    private WarrantyActivationOutboxRepository outboxRepository;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private WarrantyClient warrantyClient;
    private WarrantyActivationService service;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(WarrantyActivationOutboxRepository.class);
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        warrantyClient = mock(WarrantyClient.class);
        service = new WarrantyActivationService(outboxRepository, orderRepository, paymentRepository, warrantyClient);
    }

    @Test
    void paidEligibleOrderEnqueuesOneActivationByOrderItemIdentity() {
        OrderItem item = OrderItem.builder().id(23L).skuId("FRM-001").build();
        Order order = Order.builder().id(8L).orderCode("ORD-8").items(java.util.List.of(item)).build();
        when(paymentRepository.existsByOrderIdAndStatus(8L, PaymentStatus.SUCCESS)).thenReturn(true);
        when(outboxRepository.findByOrderItemIdAndBenefitType(23L, "FRAME_ADJUSTMENT"))
                .thenReturn(Optional.empty());

        service.enqueueForPaidOrder(order);

        verify(outboxRepository).save(argThat(outbox ->
                outbox.getOrderId().equals(8L)
                        && outbox.getOrderItemId().equals(23L)
                        && outbox.getBenefitType().equals("FRAME_ADJUSTMENT")
                        && outbox.getIdempotencyKey().equals("WARRANTY-8-23-FRAME_ADJUSTMENT")));
    }

    @Test
    void unpaidOrderDoesNotEnqueueWarranty() {
        Order order = Order.builder().id(8L).items(java.util.List.of(
                OrderItem.builder().id(23L).skuId("FRM-001").build())).build();
        when(paymentRepository.existsByOrderIdAndStatus(8L, PaymentStatus.SUCCESS)).thenReturn(false);

        service.enqueueForPaidOrder(order);

        verify(outboxRepository, never()).save(any(WarrantyActivationOutbox.class));
    }

    @Test
    void retriesPendingOutboxAndPreservesRetryableFailure() {
        WarrantyActivationOutbox pending = WarrantyActivationOutbox.builder()
                .id(1L).orderId(8L).orderItemId(23L).benefitType("FRAME_ADJUSTMENT")
                .idempotencyKey("key").status("PENDING").build();
        when(outboxRepository.findTop20ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(java.util.List.of(pending));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(warrantyClient.activate(any())).thenThrow(new RuntimeException("lab unavailable"));

        service.retryPendingActivations();

        assertThat(pending.getStatus()).isEqualTo("PENDING");
        assertThat(pending.getAttempts()).isEqualTo(1);
        assertThat(pending.getLastError()).contains("lab unavailable");
        verify(outboxRepository, atLeastOnce()).save(pending);
    }
}
