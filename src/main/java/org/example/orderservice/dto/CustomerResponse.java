package org.example.orderservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
