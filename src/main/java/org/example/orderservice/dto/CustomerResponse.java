package org.example.orderservice.dto;

import org.example.orderservice.model.CustomerRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        CustomerRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
