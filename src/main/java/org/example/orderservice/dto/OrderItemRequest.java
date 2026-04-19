package org.example.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank @Size(max = 255) String productId,
        @NotBlank @Size(max = 255) String productName,
        @Min(1) @Max(10_000) int quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {
}
