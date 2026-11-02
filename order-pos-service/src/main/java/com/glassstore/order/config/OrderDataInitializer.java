package com.glassstore.order.config;

import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDataInitializer implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) {
        log.info("Order Service khởi động ở trạng thái sạch (0 đơn hàng giả lập). Sẵn sàng nhận đơn thật từ quầy POS & Web!");
    }
}
