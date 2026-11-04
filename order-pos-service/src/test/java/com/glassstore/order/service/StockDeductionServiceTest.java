package com.glassstore.order.service;

import com.glassstore.order.client.CatalogInventoryClient;
import com.glassstore.order.domain.PaymentStatus;
import com.glassstore.order.dto.CatalogStockDeductionResponse;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.entity.StockDeductionOutbox;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.repository.StockDeductionOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockDeductionServiceTest {

    @Mock
    private StockDeductionOutboxRepository outboxRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CatalogInventoryClient catalogInventoryClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StockDeductionService stockDeductionService;

    @Test
    void successfulPaymentOutboxSendsOrderItemsAndMarksOrderSynced() {
        Order order = Order.builder()
                .id(42L)
                .branchName("Chi nhánh 01 - Lê Thánh Tôn")
                .items(List.of(OrderItem.builder().skuId("FRAME-001").quantity(2).build()))
                .build();
        Payment payment = Payment.builder()
                .orderId(42L)
                .idempotencyKey("pos-request-42")
                .transactionCode("TXN-42")
                .status(PaymentStatus.SUCCESS)
                .build();
        StockDeductionOutbox outbox = StockDeductionOutbox.builder()
                .id(7L)
                .orderId(42L)
                .idempotencyKey("pos-request-42")
                .status("PENDING")
                .attempts(0)
                .build();

        when(outboxRepository.findById(7L)).thenReturn(Optional.of(outbox));
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(42L)).thenReturn(List.of(payment));
        when(catalogInventoryClient.deductPaidOrder(any()))
                .thenReturn(new CatalogStockDeductionResponse(42L, "BRANCH_01", false));

        stockDeductionService.processAfterCommit(new StockDeductionRequestedEvent(7L, order));

        assertEquals("SYNCED", order.getInventorySyncStatus());
        assertEquals("SYNCED", outbox.getStatus());
        ArgumentCaptor<com.glassstore.order.dto.CatalogStockDeductionRequest> requestCaptor =
                ArgumentCaptor.forClass(com.glassstore.order.dto.CatalogStockDeductionRequest.class);
        verify(catalogInventoryClient).deductPaidOrder(requestCaptor.capture());
        assertEquals(42L, requestCaptor.getValue().orderId());
        assertEquals("FRAME-001", requestCaptor.getValue().items().get(0).sku());
        verify(outboxRepository).save(outbox);
    }
}
