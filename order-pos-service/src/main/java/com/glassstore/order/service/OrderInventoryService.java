package com.glassstore.order.service;

import com.glassstore.order.client.CatalogClient;
import com.glassstore.order.client.CatalogContracts;
import com.glassstore.order.domain.OrderStatus;
import com.glassstore.order.entity.Order;
import com.glassstore.order.entity.OrderItem;
import com.glassstore.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coordinates stock with catalog-service around an order's life (FR-INV-03/04/05).
 *
 * <p>Stock operations are intentionally <em>outside</em> the order transaction: they are
 * remote calls that can partially succeed, so calling them inside a local transaction would
 * give a false sense of atomicity. Each step is idempotent (keyed on the order code) and a
 * failure is surfaced to the caller instead of being swallowed.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderInventoryService {

    private final CatalogClient catalogClient;
    private final OrderRepository orderRepository;

    /** Holds stock for an order that is awaiting payment. */
    public void reserve(Order order) {
        CatalogContracts.ReserveRequest request = new CatalogContracts.ReserveRequest();
        request.setReservationKey(order.getOrderCode());
        request.setOrderCode(order.getOrderCode());
        request.setActor("SYSTEM");
        request.setLines(order.getItems().stream()
                .map(i -> new CatalogContracts.ReservationLine(i.getSkuId(), i.getQuantity()))
                .collect(Collectors.toList()));

        CatalogContracts.ApiEnvelope<CatalogContracts.ReservationResult> response =
                catalogClient.reserve(request);
        log.info("Reserved stock for order {}: {}", order.getOrderCode(),
                response != null && response.getData() != null ? response.getData().getStatus() : "unknown");
    }

    /** Returns held stock after a cancellation or payment failure. */
    public void release(Order order, String reason) {
        CatalogContracts.ReleaseRequest request = new CatalogContracts.ReleaseRequest();
        request.setReservationKey(order.getOrderCode());
        request.setActor("SYSTEM");
        request.setReason(reason);
        catalogClient.release(request);
        log.info("Released stock for order {}: {}", order.getOrderCode(), reason);
    }

    /** Converts the hold into a real deduction once money is confirmed. */
    public void commit(Order order) {
        CatalogContracts.CommitRequest request = new CatalogContracts.CommitRequest();
        request.setReservationKey(order.getOrderCode());
        request.setActor("SYSTEM");
        catalogClient.commit(request);
        log.info("Committed stock for order {}", order.getOrderCode());
    }

    /**
     * Applies the stock effect of a status change.
     *
     * <p>Rules mirrors the business decision that money and goods move together: a paid order
     * deducts stock, a cancelled unpaid order releases it, a cancelled paid order releases the
     * hold and the refund path handles the money (OQ-11).</p>
     */
    public void onStatusChanged(Order order, OrderStatus from, OrderStatus to) {
        try {
            if (to == OrderStatus.PAID) {
                commit(order);
            } else if (to == OrderStatus.CANCELLED) {
                boolean wasPaid = from != null && from != OrderStatus.PENDING_PAYMENT;
                release(order, wasPaid
                        ? "Hủy đơn đã thanh toán - hoàn tồn kho"
                        : "Hủy đơn chưa thanh toán");
            }
        } catch (Exception ex) {
            // Never let a remote hiccup silently leave stock in a wrong state unnoticed.
            log.error("Stock sync failed for order {} ({} -> {}): {}",
                    order.getOrderCode(), from, to, ex.getMessage());
            throw ex;
        }
    }

    /** SKUs of an order, used for diagnostics. */
    public List<String> skusOf(Order order) {
        return order.getItems().stream().map(OrderItem::getSkuId).collect(Collectors.toList());
    }

    /** Small helper so callers can build a diagnostic payload consistently. */
    public Map<String, Object> snapshot(Order order) {
        return Map.of(
                "orderCode", order.getOrderCode(),
                "status", order.currentStatus().name(),
                "skus", skusOf(order));
    }
}