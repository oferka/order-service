package org.example.orderservice.event;

import org.example.orderservice.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant timestamp
) {
}
