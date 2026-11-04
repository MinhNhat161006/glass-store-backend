package com.glassstore.order.service;

import com.glassstore.order.client.LabClient;
import com.glassstore.order.domain.*;
import com.glassstore.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class LabDispatchWorker {
    private final LabDispatchOutboxRepository outbox;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final LabClient lab;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long id) {
        var entry = outbox.lockById(id).orElseThrow();
        if (!"PENDING".equals(entry.getStatus())) return;
        var order = orders.findById(entry.getOrderId()).orElseThrow();
        if (order.getStatus() == OrderStatus.CANCEL_REQUESTED) {
            entry.setLastError("Đơn đang chờ xử lý yêu cầu hủy"); return;
        }
        if (Set.of(OrderStatus.CANCELLED,OrderStatus.CANCEL_PENDING,OrderStatus.REFUND_PENDING,OrderStatus.REFUNDED,OrderStatus.COMPLETED).contains(order.getStatus())
                || !payments.existsByOrderIdAndStatus(order.getId(),PaymentStatus.SUCCESS)) {
            entry.setStatus("SKIPPED"); return;
        }
        entry.setAttempts(entry.getAttempts()+1);
        try {
            Map<String,Object> payload = new LinkedHashMap<>();
            payload.put("orderId",order.getId()); payload.put("prescriptionId",order.getPrescriptionId());
            payload.put("customerName",order.getCustomerName());
            for (var item : order.getItems()) {
                String sku = Objects.toString(item.getSkuId(),"").toUpperCase(Locale.ROOT);
                if (sku.startsWith("LNS-") || sku.startsWith("LZ-")) {
                    payload.put("lensSku",item.getSkuId()); payload.put("lensName",item.getProductName());
                } else if (!sku.startsWith("ACC-")) {
                    payload.put("frameSku",item.getSkuId()); payload.put("frameName",item.getProductName());
                }
            }
            var response = lab.createFromOrder(payload);
            Object jobId = response == null || response.getData() == null ? null : response.getData().get("id");
            if (jobId == null) throw new IllegalStateException("Lab chưa trả về mã lệnh đã lưu");
            entry.setLabJobId(Long.valueOf(jobId.toString())); entry.setStatus("SYNCED");
            entry.setLastError(null); entry.setProcessedAt(LocalDateTime.now());
        } catch (RuntimeException error) {
            String message = Objects.toString(error.getMessage(),"Lab unavailable");
            entry.setLastError(message.substring(0,Math.min(message.length(),1000)));
            log.warn("Lab dispatch for order {} remains pending and will retry: {}",order.getId(),message);
        }
    }
}
