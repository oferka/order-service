package org.example.orderservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ExternalInventoryClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalInventoryClient.class);
    private static final double FAILURE_RATE = 0.3;

    @Retryable(
            retryFor = RuntimeException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public InventoryCheckResponse checkInventory(String productId, int quantity) {
        log.debug("Checking inventory for productId={}, quantity={}", productId, quantity);

        if (Math.random() < FAILURE_RATE) {
            throw new RuntimeException("Inventory service unavailable (simulated failure)");
        }

        // Simulated successful downstream response
        return new InventoryCheckResponse(productId, true, quantity);
    }

    @Recover
    public InventoryCheckResponse recoverCheckInventory(RuntimeException e, String productId, int quantity) {
        log.warn("Inventory check failed after all retries for productId={}, quantity={}: {}",
                productId, quantity, e.getMessage());
        // Fallback: assume stock is available to avoid blocking order creation
        return new InventoryCheckResponse(productId, true, quantity);
    }
}
