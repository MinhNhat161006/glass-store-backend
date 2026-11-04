package com.glassstore.order.service;

import com.glassstore.order.client.CatalogInventoryClient;
import com.glassstore.order.dto.CatalogStockDeductionRequest;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.Payment;
import com.glassstore.order.entity.StockDeductionOutbox;
import com.glassstore.order.repository.OrderRepository;
import com.glassstore.order.repository.PaymentRepository;
import com.glassstore.order.repository.StockDeductionOutboxRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockDeductionService {

    private final StockDeductionOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CatalogInventoryClient catalogInventoryClient;
    private final ApplicationEventPublisher eventPublisher;

    public void enqueue(Order order, Payment payment) {
        StockDeductionOutbox outbox = outboxRepository.save(StockDeductionOutbox.builder()
                .orderId(order.getId())
                .idempotencyKey(payment.getIdempotencyKey())
                .status("PENDING")
                .build());
        order.setInventorySyncStatus("PENDING");
        eventPublisher.publishEvent(new StockDeductionRequestedEvent(outbox.getId(), order));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAfterCommit(StockDeductionRequestedEvent event) {
        event.order().setInventorySyncStatus(processOutbox(event.outboxId()));
    }

    @Scheduled(fixedDelayString = "${inventory.outbox.poll-interval-ms:5000}")
    public void retryPendingDeductions() {
        outboxRepository.findTop20ByStatusOrderByCreatedAtAsc("PENDING")
                .forEach(outbox -> processOutbox(outbox.getId()));
    }

    public String statusForOrder(Long orderId) {
        return outboxRepository.findByOrderId(orderId)
                .map(StockDeductionOutbox::getStatus)
                .orElse("NOT_REQUIRED");
    }

    private String processOutbox(Long outboxId) {
        StockDeductionOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("Stock deduction outbox event was not found: " + outboxId));
        if ("SYNCED".equals(outbox.getStatus()) || "FAILED".equals(outbox.getStatus())) {
            return outbox.getStatus();
        }

        Order order = orderRepository.findById(outbox.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Paid order was not found for stock deduction: " + outbox.getOrderId()));
        Payment payment = paymentRepository.findByOrderId(order.getId()).stream()
                .filter(candidate -> candidate.getStatus() == com.glassstore.order.domain.PaymentStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Successful payment was not found for order " + order.getId()));
        CatalogStockDeductionRequest request = new CatalogStockDeductionRequest(
                order.getId(),
                outbox.getIdempotencyKey(),
                payment.getTransactionCode(),
                order.getBranchName(),
                order.getItems().stream()
                        .map(item -> new CatalogStockDeductionRequest.Item(item.getSkuId(), item.getQuantity()))
                        .toList()
        );

        outbox.setAttempts(outbox.getAttempts() + 1);
        outbox.setLastAttemptAt(LocalDateTime.now());
        try {
            catalogInventoryClient.deductPaidOrder(request);
            outbox.setStatus("SYNCED");
            outbox.setProcessedAt(LocalDateTime.now());
            outbox.setLastError(null);
            outboxRepository.save(outbox);
            return "SYNCED";
        } catch (FeignException exception) {
            String error = exception.getMessage() == null ? "Catalog service request failed" : exception.getMessage();
            outbox.setLastError(error.substring(0, Math.min(error.length(), 1000)));
            if (exception.status() >= 400 && exception.status() < 500) {
                outbox.setStatus("FAILED");
                log.error("Stock deduction rejected for paid order {}: {}", order.getId(), error);
            } else {
                log.warn("Stock deduction pending for paid order {}; it will be retried: {}", order.getId(), error);
            }
            outboxRepository.save(outbox);
            return outbox.getStatus();
        }
    }
}
