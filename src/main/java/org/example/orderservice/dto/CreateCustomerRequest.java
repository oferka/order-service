package org.example.orderservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        String phone
) {
}
