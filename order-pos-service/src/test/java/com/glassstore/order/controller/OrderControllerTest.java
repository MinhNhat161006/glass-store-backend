package com.glassstore.order.controller;

import com.glassstore.order.domain.OrderStatus;
import com.glassstore.order.domain.PaymentMethod;
import com.glassstore.order.dto.OrderDtos;
import com.glassstore.order.entity.Order;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.OrderStatusHistoryRepository;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.service.OrderCheckoutService;
import com.glassstore.order.service.OrderStatusService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @Test
    void posVietQrCheckoutMarksOrderPaidAfterCounterConfirmation() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        OrderStatusHistoryRepository historyRepository = mock(OrderStatusHistoryRepository.class);
        OrderCheckoutService checkoutService = mock(OrderCheckoutService.class);
        OrderStatusService orderStatusService = mock(OrderStatusService.class);
        OrderController controller = new OrderController(orderRepository, paymentRepository,
                historyRepository, checkoutService, orderStatusService);

        Order pendingOrder = Order.builder()
                .id(12L)
                .orderCode("ORD-POS-12")
                .paymentMethod(PaymentMethod.VIETQR)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        Order paidOrder = Order.builder()
                .id(12L)
                .orderCode("ORD-POS-12")
                .paymentMethod(PaymentMethod.VIETQR)
                .status(OrderStatus.PAID)
                .build();
        OrderDtos.CheckoutRequest request = OrderDtos.CheckoutRequest.builder()
                .customerName("Khách vãng lai")
                .paymentMethod("VIETQR")
                .channel("POS")
                .staffName("Thu ngân")
                .build();
        when(checkoutService.checkout(request, "pos-request-12"))
                .thenReturn(new OrderCheckoutService.CheckoutOutcome(pendingOrder, false));
        when(checkoutService.markPaid(pendingOrder, "Thu ngân",
                "Đã xác nhận nhận tiền chuyển khoản tại quầy", "VIETQR"))
                .thenReturn(paidOrder);

        var response = controller.createOrder("pos-request-12", request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.PAID);
        verify(checkoutService).markPaid(pendingOrder, "Thu ngân",
                "Đã xác nhận nhận tiền chuyển khoản tại quầy", "VIETQR");
    }
}
