package org.example.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import org.example.orderservice.model.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
}
