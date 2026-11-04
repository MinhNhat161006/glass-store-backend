package com.glassstore.order.service;
import com.glassstore.order.entity.*;
import com.glassstore.order.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class OrderCancellationService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final StockDeductionOutboxRepository outboxes;
    private final PromotionService promotions;
    private final PosDataService data;
    private final ApplicationEventPublisher events;
    public record Request(String reason, boolean refundConfirmed) {}

    @Transactional
    public Order cancel(Long id, Request request, String token) {
        String actor = data.actor(token,false).path("username").asText();
        if(request==null || request.reason()==null || request.reason().isBlank() || request.reason().length()>255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nhập lý do hủy tối đa 255 ký tự.");
        Order order=orders.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!"POS".equals(order.getChannel()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chỉ hủy đơn tại quầy trong luồng này.");
        if("CANCELLED".equals(order.getStatus()))return order;
        if(!Set.of("PAID","CANCEL_PENDING").contains(order.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Đơn đã được xử lý tiếp, không thể hủy tại quầy.");
        if(!"CANCEL_PENDING".equals(order.getStatus())) {
            order.setStatus("CANCEL_PENDING");order.setCancelReason(request.reason().trim());order.setCancelledBy(actor);order.setCancelledAt(LocalDateTime.now());
            order.setRefundStatus(request.refundConfirmed()?"CONFIRMED":"PENDING");
            if(request.refundConfirmed()){order.setRefundConfirmedBy(actor);order.setRefundConfirmedAt(LocalDateTime.now());}
            orders.save(order);
            payments.findByOrderId(id).forEach(payment->{payment.setStatus(request.refundConfirmed()?"REFUNDED":"REFUND_PENDING");payments.save(payment);});
        }
        StockDeductionOutbox row=outboxes.findByOrderId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Thiếu giao dịch kho gốc, cần quản lý đối chiếu."));
        row=outboxes.lockById(row.getId()).orElseThrow();
        row.setOperation("CANCEL");row.setStatus("PENDING");row.setLastError(null);outboxes.save(row);
        order.setInventorySyncStatus("PENDING");events.publishEvent(new StockDeductionRequestedEvent(row.getId(),order));return order;
    }

    @Transactional
    public void complete(Long id) {
        Order order=orders.lockById(id).orElseThrow();
        if("CANCELLED".equals(order.getStatus()))return;
        if(!"CANCEL_PENDING".equals(order.getStatus()))throw new IllegalStateException("Order is not pending cancellation");
        promotions.reverse(order);order.setStatus("CANCELLED");orders.save(order);
    }

    @Transactional
    public Order confirmRefund(Long id,String token) {
        String actor=data.actor(token,false).path("username").asText();
        Order order=orders.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!"CANCELLED".equals(order.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chỉ xác nhận hoàn tiền sau khi hủy hoàn tất.");
        if(!"CONFIRMED".equals(order.getRefundStatus())) {
            order.setRefundStatus("CONFIRMED");order.setRefundConfirmedBy(actor);order.setRefundConfirmedAt(LocalDateTime.now());orders.save(order);
            payments.findByOrderId(id).forEach(payment->{payment.setStatus("REFUNDED");payments.save(payment);});
        }
        return order;
    }
}
