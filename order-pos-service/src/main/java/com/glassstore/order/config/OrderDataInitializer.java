package com.glassstore.order.config;

import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDataInitializer implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) {
        if (orderRepository.count() > 0) {
            log.info("Order database đã có dữ liệu đơn hàng, bỏ qua nạp seed.");
            return;
        }

        log.info("Bắt đầu khởi tạo dữ liệu 6 đơn hàng đa kênh (Online & POS)...");

        // 1. OPT-8842-SG (Nguyễn Hoàng Duy)
        Order o1 = Order.builder()
                .orderCode("OPT-8842-SG")
                .customerId(10L)
                .customerName("Nguyễn Hoàng Duy")
                .customerPhone("0918732998")
                .shippingAddress("120 Hai Bà Trưng, P. Đa Kao, Q.1, TP.HCM")
                .channel("POS")
                .orderType("COMPLETE_GLASSES")
                .totalAmount(new BigDecimal("5230000"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("5230000"))
                .status("PROCESSING")
                .paymentMethod("VNPAY")
                .staffName("STF-088 (Trần M. Anh)")
                .branchName("Flagship Q.1 (TP.HCM)")
                .notes("Khám đo mắt tại phòng 01 BS. Vũ Anh Tuấn. Kính mài hoàn tất lấy trong ngày.")
                .createdAt(LocalDateTime.now().minusHours(4))
                .build();

        OrderItem item1_1 = OrderItem.builder()
                .order(o1)
                .skuId("FRM-BOLON-BJ3120-BLK")
                .productName("Gọng Bolon Titanium - Đen Titan Nhám")
                .prescriptionId(1L)
                .quantity(1)
                .unitPrice(new BigDecimal("3250000"))
                .subtotal(new BigDecimal("3250000"))
                .build();

        OrderItem item1_2 = OrderItem.builder()
                .order(o1)
                .skuId("LNS-ESS-CRIZAL-167")
                .productName("Cặp Tròng Essilor Crizal Rock 1.67 Blue UV")
                .prescriptionId(1L)
                .quantity(1)
                .unitPrice(new BigDecimal("1980000"))
                .subtotal(new BigDecimal("1980000"))
                .build();

        o1.setItems(new ArrayList<>(List.of(item1_1, item1_2)));
        Order saved1 = orderRepository.save(o1);

        paymentRepository.save(Payment.builder()
                .orderId(saved1.getId())
                .paymentMethod("VNPAY")
                .transactionCode("VNP-20261102-8842")
                .amount(saved1.getFinalAmount())
                .status("SUCCESS")
                .paidAt(LocalDateTime.now().minusHours(4))
                .build());

        // 2. OPT-8841-ONL (Lê Thúy Kiều)
        Order o2 = Order.builder()
                .orderCode("OPT-8841-ONL")
                .customerId(11L)
                .customerName("Lê Thúy Kiều")
                .customerPhone("0988124551")
                .shippingAddress("45 Lê Duẩn, P. Bến Nghé, Q.1, TP.HCM")
                .channel("ONLINE")
                .orderType("COMPLETE_GLASSES")
                .totalAmount(new BigDecimal("4750000"))
                .discountAmount(new BigDecimal("630000"))
                .finalAmount(new BigDecimal("4120000"))
                .status("SHIPPING")
                .paymentMethod("VIETQR")
                .staffName("E-Com Team")
                .branchName("Flagship Q.1 (TP.HCM)")
                .notes("Đơn đặt hàng qua App OptiVision. Giao hỏa tốc AhaMove.")
                .createdAt(LocalDateTime.now().minusHours(6))
                .build();

        OrderItem item2_1 = OrderItem.builder()
                .order(o2)
                .skuId("FRM-RB-AVIATOR-GLD")
                .productName("Gọng Ray-Ban Aviator - Mạ Vàng Kim 18K")
                .prescriptionId(2L)
                .quantity(1)
                .unitPrice(new BigDecimal("4100000"))
                .subtotal(new BigDecimal("4100000"))
                .build();

        OrderItem item2_2 = OrderItem.builder()
                .order(o2)
                .skuId("LNS-CHEMI-U2-160")
                .productName("Cặp Tròng Chemi U2 BlueCut 1.60")
                .prescriptionId(2L)
                .quantity(1)
                .unitPrice(new BigDecimal("650000"))
                .subtotal(new BigDecimal("650000"))
                .build();

        o2.setItems(new ArrayList<>(List.of(item2_1, item2_2)));
        Order saved2 = orderRepository.save(o2);

        paymentRepository.save(Payment.builder()
                .orderId(saved2.getId())
                .paymentMethod("VIETQR")
                .transactionCode("VQR-MBB-988124")
                .amount(saved2.getFinalAmount())
                .status("SUCCESS")
                .paidAt(LocalDateTime.now().minusHours(6))
                .build());

        // 3. OPT-8839-WEB (Trịnh Minh Quang)
        Order o3 = Order.builder()
                .orderCode("OPT-8839-WEB")
                .customerId(12L)
                .customerName("Trịnh Minh Quang")
                .customerPhone("0912449002")
                .shippingAddress("88 Nguyễn Đình Chiểu, Q.3, TP.HCM")
                .channel("ONLINE")
                .orderType("COMPLETE_GLASSES")
                .totalAmount(new BigDecimal("6850000"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("6850000"))
                .status("PROCESSING")
                .paymentMethod("VIETQR")
                .staffName("Web Team")
                .branchName("Flagship Q.1 (TP.HCM)")
                .notes("Độ cận cao -7.50, mài tròng mỏng 1.74 chiết suất cao.")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        OrderItem item3_1 = OrderItem.builder()
                .order(o3)
                .skuId("FRM-CHARMANT-ZT2233")
                .productName("Gọng Charmant Titanium Z Khoan Không Viền")
                .prescriptionId(3L)
                .quantity(1)
                .unitPrice(new BigDecimal("4350000"))
                .subtotal(new BigDecimal("4350000"))
                .build();

        OrderItem item3_2 = OrderItem.builder()
                .order(o3)
                .skuId("LNS-CHEMI-U6-174")
                .productName("Cặp Tròng Chemi Crystal U6 1.74 Siêu Mỏng")
                .prescriptionId(3L)
                .quantity(1)
                .unitPrice(new BigDecimal("2500000"))
                .subtotal(new BigDecimal("2500000"))
                .build();

        o3.setItems(new ArrayList<>(List.of(item3_1, item3_2)));
        Order saved3 = orderRepository.save(o3);

        paymentRepository.save(Payment.builder()
                .orderId(saved3.getId())
                .paymentMethod("VIETQR")
                .transactionCode("VQR-VCB-912449")
                .amount(saved3.getFinalAmount())
                .status("SUCCESS")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build());

        // 4. OPT-8835-HN (Phạm Thị Bích Ngọc)
        Order o4 = Order.builder()
                .orderCode("OPT-8835-HN")
                .customerId(13L)
                .customerName("Phạm Thị Bích Ngọc")
                .customerPhone("0977898332")
                .shippingAddress("24 Tràng Tiền, Hoàn Kiếm, Hà Nội")
                .channel("POS")
                .orderType("COMPLETE_GLASSES")
                .totalAmount(new BigDecimal("12500000"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("12500000"))
                .status("PROCESSING")
                .paymentMethod("VNPAY")
                .staffName("Lê Văn Hà (STF-012)")
                .branchName("Tràng Tiền Plaza (Hà Nội)")
                .notes("Kính đa tròng kỹ thuật số cao cấp Varilux.")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        OrderItem item4_1 = OrderItem.builder()
                .order(o4)
                .skuId("FRM-BOLON-BJ7150-ROSE")
                .productName("Gọng Đa Giác Bolon Retro Chic - Vàng Hồng")
                .prescriptionId(4L)
                .quantity(1)
                .unitPrice(new BigDecimal("3650000"))
                .subtotal(new BigDecimal("3650000"))
                .build();

        OrderItem item4_2 = OrderItem.builder()
                .order(o4)
                .skuId("LNS-ESS-VARILUX-160")
                .productName("Cặp Tròng Đa Tròng Essilor Varilux Comfort Max 1.60")
                .prescriptionId(4L)
                .quantity(1)
                .unitPrice(new BigDecimal("5650000"))
                .subtotal(new BigDecimal("5650000"))
                .build();

        OrderItem item4_3 = OrderItem.builder()
                .order(o4)
                .skuId("FRM-CHARMANT-ZT2233")
                .productName("Gọng Charmant Titanium Z Dự Phòng")
                .quantity(1)
                .unitPrice(new BigDecimal("3200000"))
                .subtotal(new BigDecimal("3200000"))
                .build();

        o4.setItems(new ArrayList<>(List.of(item4_1, item4_2, item4_3)));
        Order saved4 = orderRepository.save(o4);

        paymentRepository.save(Payment.builder()
                .orderId(saved4.getId())
                .paymentMethod("VNPAY")
                .transactionCode("POS-BIDV-883501")
                .amount(saved4.getFinalAmount())
                .status("SUCCESS")
                .paidAt(LocalDateTime.now().minusDays(2))
                .build());

        log.info("Đã nạp thành công {} đơn hàng vào db_order!", orderRepository.count());
    }
}
