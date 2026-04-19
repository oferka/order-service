package org.example.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        BigDecimal totalAmount,
        Instant timestamp,
        String correlationId
) {
}
