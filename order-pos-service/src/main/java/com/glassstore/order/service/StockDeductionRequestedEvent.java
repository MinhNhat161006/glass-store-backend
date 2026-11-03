package com.glassstore.order.service;

import com.glassstore.order.entity.Order;

public record StockDeductionRequestedEvent(Long outboxId, Order order) {
}
