package org.example.orderservice.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final String timestamp;
    private final String correlationId;
    private final List<FieldError> errors;

    public static String now() {
        return Instant.now().toString();
    }

    public record FieldError(String field, String message) {
    }
}
