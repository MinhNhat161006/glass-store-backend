package com.glassstore.order.service;
import com.glassstore.order.domain.*;
import com.glassstore.order.client.CatalogInventoryClient;
import com.glassstore.order.dto.CatalogStockDeductionRequest;
import com.glassstore.order.entity.*;
import com.glassstore.order.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class StockOutboxWorker {
    private final StockDeductionOutboxRepository outboxes;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final CatalogInventoryClient catalog;
    private final OrderCancellationService cancellations;

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public String process(Long id) {
        Long orderId=outboxes.findOrderIdById(id).orElseThrow();
        // Lock order first in both worker and cancellation to serialize an in-flight sale with a cancel.
        Order order=orders.lockById(orderId).orElseThrow();
        StockDeductionOutbox row=outboxes.lockById(id).orElseThrow();
        if(!"PENDING".equals(row.getStatus()))return row.getStatus();
        boolean cancel="CANCEL".equals(row.getOperation());
        if(!cancel && !(order.getStatus() == OrderStatus.PAID)) {row.setStatus("FAILED");row.setLastError("Đơn không còn trạng thái đã thanh toán.");outboxes.save(row);return "FAILED";}
        Payment payment=payments.findByOrderId(order.getId()).stream().filter(p->row.getIdempotencyKey().equals(p.getIdempotencyKey())).findFirst().orElseThrow();
        CatalogStockDeductionRequest request=new CatalogStockDeductionRequest(order.getId(),row.getIdempotencyKey(),payment.getTransactionCode(),order.getBranchCode()!=null?order.getBranchCode():order.getBranchName(),order.getItems().stream().map(item->new CatalogStockDeductionRequest.Item(item.getSkuId(),item.getQuantity())).toList());
        row.setAttempts(row.getAttempts()+1);row.setLastAttemptAt(LocalDateTime.now());
        try {
            if(cancel){catalog.cancelSale(request);cancellations.complete(order.getId());}
            else catalog.deductPaidOrder(request);
            row.setStatus("SYNCED");row.setProcessedAt(LocalDateTime.now());row.setLastError(null);
        } catch(feign.FeignException error) {
            String message=error.getMessage()==null?"Không kết nối được kho":error.getMessage();row.setLastError(message.substring(0,Math.min(1000,message.length())));
            row.setStatus(error.status()>=400&&error.status()<500?"FAILED":"PENDING");
        }
        outboxes.save(row);return row.getStatus();
    }
}
