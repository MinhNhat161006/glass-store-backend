package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.domain.OrderStatus;
import com.glassstore.order.domain.PaymentStatus;
import com.glassstore.order.dto.WarrantyOrderItemDTO;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.WarrantyActivationOutbox;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.repository.WarrantyActivationOutboxRepository;
import com.glassstore.order.repository.WarrantyOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WarrantyOrderLookupService {

    private static final Set<OrderStatus> INVALID_PURCHASE_STATUSES =
            Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED, OrderStatus.REFUND_PENDING, OrderStatus.REFUNDED);

    private final WarrantyOrderItemRepository warrantyOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final WarrantyActivationOutboxRepository outboxRepository;

    @Transactional(readOnly = true)
    public List<WarrantyOrderItemDTO> findPurchases(String phone, String sku) {
        List<Order> orders = phone != null
                ? warrantyOrderItemRepository.findOrdersWithItemsByCustomerPhone(phone.trim())
                : warrantyOrderItemRepository.findOrdersWithItemsBySku(sku.trim());
        return orders.stream()
                .filter(this::isPaidPurchase)
                .flatMap(order -> order.getItems().stream()
                        .filter(item -> sku == null || item.getSkuId().equalsIgnoreCase(sku.trim()))
                        .map(item -> toDto(order, item)))
                .toList();
    }

    @Transactional(readOnly = true)
    public WarrantyOrderItemDTO requirePurchaseItem(Long orderId, Long orderItemId) {
        Order order = warrantyOrderItemRepository.findById(orderId)
                .orElseThrow(() -> ApiException.of(ErrorCode.ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng: " + orderId));
        if (!isPaidPurchase(order)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Đơn hàng chưa thanh toán hoặc không còn hợp lệ");
        }
        OrderItem item = order.getItems().stream()
                .filter(candidate -> candidate.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> ApiException.of(ErrorCode.VALIDATION_ERROR,
                        "Sản phẩm không thuộc đơn hàng đã chọn"));
        return toDto(order, item);
    }

    private boolean isPaidPurchase(Order order) {
        return !INVALID_PURCHASE_STATUSES.contains(order.currentStatus())
                && paymentRepository.existsByOrderIdAndStatus(order.getId(), PaymentStatus.SUCCESS);
    }

    private WarrantyOrderItemDTO toDto(Order order, OrderItem item) {
        List<String> benefits = WarrantyActivationServiceBenefits.forSku(item.getSkuId());
        List<WarrantyActivationOutbox> outboxEntries =
                outboxRepository.findByOrderIdOrderByOrderItemIdAsc(order.getId()).stream()
                        .filter(entry -> entry.getOrderItemId().equals(item.getId()))
                        .toList();
        if (benefits.isEmpty()) {
            return new WarrantyOrderItemDTO(order.getId(), item.getId(), order.getOrderCode(), order.getCustomerId(),
                    order.getCustomerName(), order.getCustomerPhone(), item.getSkuId(), item.getProductName(),
                    order.getCreatedAt(), order.currentStatus().name(), null, "NOT_ELIGIBLE", 0, null);
        }
        String benefit = benefits.get(0);
        WarrantyActivationOutbox activation = outboxEntries.stream()
                .filter(entry -> entry.getBenefitType().equals(benefit))
                .findFirst().orElse(null);
        return new WarrantyOrderItemDTO(order.getId(), item.getId(), order.getOrderCode(), order.getCustomerId(),
                order.getCustomerName(), order.getCustomerPhone(), item.getSkuId(), item.getProductName(),
                order.getCreatedAt(), order.currentStatus().name(), benefit,
                activation == null ? "NOT_REQUESTED" : activation.getStatus(),
                activation == null ? 0 : activation.getAttempts(),
                activation == null ? null : activation.getLastError());
    }

    private static final class WarrantyActivationServiceBenefits {
        private WarrantyActivationServiceBenefits() {
        }

        static List<String> forSku(String sku) {
            if (sku == null) {
                return List.of();
            }
            String normalized = sku.trim().toUpperCase();
            if (normalized.startsWith("FRM-")) {
                return List.of("FRAME_ADJUSTMENT");
            }
            if (normalized.startsWith("LNS-") || normalized.startsWith("LZ-")) {
                return List.of("LENS_COATING");
            }
            return List.of();
        }
    }
}
