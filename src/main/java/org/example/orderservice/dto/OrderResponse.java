package org.example.orderservice.dto;

import org.example.orderservice.model.OrderStatus;
import org.example.orderservice.model.ShippingAddress;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        String customerEmail,
        String customerName,
        List<OrderItemResponse> items,
        ShippingAddress shippingAddress,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
