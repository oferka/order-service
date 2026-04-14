package org.example.orderservice.client;

public record InventoryCheckResponse(
        String productId,
        boolean available,
        int availableQuantity
) {
}
