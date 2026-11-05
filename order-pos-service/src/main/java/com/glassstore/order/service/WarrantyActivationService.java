package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.WarrantyClient;
import com.glassstore.order.domain.OrderStatus;
import com.glassstore.order.domain.PaymentStatus;
import com.glassstore.order.dto.WarrantyActivationRequest;
import com.glassstore.order.dto.WarrantyActivationStatusDTO;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.entity.WarrantyActivationOutbox;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.repository.WarrantyActivationOutboxRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarrantyActivationService {

    private final WarrantyActivationOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WarrantyClient warrantyClient;

    @Transactional
    public void enqueueForPaidOrder(Order order) {
        if (!paymentRepository.existsByOrderIdAndStatus(order.getId(), PaymentStatus.SUCCESS)) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            for (String benefitType : benefitTypes(item.getSkuId())) {
                if (outboxRepository.findByOrderItemIdAndBenefitType(item.getId(), benefitType).isEmpty()) {
                    outboxRepository.save(WarrantyActivationOutbox.builder()
                            .orderId(order.getId())
                            .orderItemId(item.getId())
                            .benefitType(benefitType)
                            .idempotencyKey("WARRANTY-" + order.getId() + "-" + item.getId() + "-" + benefitType)
                            .status("PENDING")
                            .build());
                }
            }
        }
    }

    @Transactional
    public List<WarrantyActivationStatusDTO> retryForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.of(ErrorCode.ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng: " + orderId));
        if (order.currentStatus() == OrderStatus.PENDING_PAYMENT
                || order.currentStatus() == OrderStatus.CANCELLED
                || order.currentStatus() == OrderStatus.REFUND_PENDING
                || order.currentStatus() == OrderStatus.REFUNDED
                || !paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "Chỉ có thể kích hoạt bảo hành cho đơn đã thanh toán hợp lệ");
        }

        enqueueForPaidOrder(order);
        List<WarrantyActivationOutbox> entries = outboxRepository.findByOrderIdOrderByOrderItemIdAsc(orderId);
        if (entries.isEmpty()) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "Đơn hàng không có sản phẩm đủ điều kiện bảo hành");
        }
        for (WarrantyActivationOutbox entry : entries) {
            if ("FAILED".equals(entry.getStatus())) {
                entry.setStatus("PENDING");
                entry.setLastError(null);
                entry.setProcessedAt(null);
            }
        }
        outboxRepository.saveAll(entries);
        return entries.stream().map(this::toStatus).toList();
    }

    @Transactional(readOnly = true)
    public List<WarrantyActivationStatusDTO> statusesForOrder(Long orderId) {
        return outboxRepository.findByOrderIdOrderByOrderItemIdAsc(orderId).stream()
                .map(this::toStatus)
                .toList();
    }

    @Scheduled(fixedDelayString = "${warranty.outbox.poll-interval-ms:5000}")
    public void retryPendingActivations() {
        outboxRepository.findTop20ByStatusOrderByCreatedAtAsc("PENDING")
                .forEach(entry -> process(entry.getId()));
    }

    private void process(Long outboxId) {
        WarrantyActivationOutbox entry = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy yêu cầu kích hoạt bảo hành " + outboxId));
        if (!"PENDING".equals(entry.getStatus())) {
            return;
        }
        entry.setAttempts(entry.getAttempts() + 1);
        entry.setLastAttemptAt(LocalDateTime.now());
        outboxRepository.save(entry);
        try {
            warrantyClient.activate(new WarrantyActivationRequest(
                    entry.getOrderId(), entry.getOrderItemId(), entry.getBenefitType()));
            entry.setStatus("SYNCED");
            entry.setLastError(null);
            entry.setProcessedAt(LocalDateTime.now());
        } catch (FeignException exception) {
            String error = exception.getMessage() == null ? "Warranty service request failed" : exception.getMessage();
            entry.setLastError(truncate(error));
            if (exception.status() >= 400 && exception.status() < 500) {
                entry.setStatus("FAILED");
                log.error("Warranty activation rejected for order item {}: {}", entry.getOrderItemId(), error);
            } else {
                log.warn("Warranty activation for order item {} will be retried: {}",
                        entry.getOrderItemId(), error);
            }
        } catch (RuntimeException exception) {
            String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            entry.setLastError(truncate(error));
            log.warn("Warranty activation for order item {} will be retried: {}",
                    entry.getOrderItemId(), error);
        }
        outboxRepository.save(entry);
    }

    private WarrantyActivationStatusDTO toStatus(WarrantyActivationOutbox entry) {
        return new WarrantyActivationStatusDTO(entry.getOrderItemId(), entry.getBenefitType(), entry.getStatus(),
                entry.getAttempts(), entry.getLastError(), entry.getLastAttemptAt(), entry.getProcessedAt());
    }

    private List<String> benefitTypes(String sku) {
        if (sku == null) {
            return List.of();
        }
        String normalized = sku.trim().toUpperCase();
        List<String> benefits = new ArrayList<>(1);
        if (normalized.startsWith("FRM-")) {
            benefits.add("FRAME_ADJUSTMENT");
        } else if (normalized.startsWith("LNS-") || normalized.startsWith("LZ-")) {
            benefits.add("LENS_COATING");
        }
        if (!benefits.isEmpty()) benefits.add("EXCHANGE_30_DAYS");
        return benefits;
    }

    private String truncate(String value) {
        return value.substring(0, Math.min(value.length(), 1000));
    }
}
