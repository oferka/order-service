package org.example.orderservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order created: orderId={}, orderNumber={}, customerId={}, totalAmount={}, timestamp={}",
                event.orderId(), event.orderNumber(), event.customerId(), event.totalAmount(), event.timestamp());
        // TODO: send order confirmation email
        // TODO: notify inventory service
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order status changed: orderId={}, orderNumber={}, {} -> {}, timestamp={}",
                event.orderId(), event.orderNumber(), event.previousStatus(), event.newStatus(), event.timestamp());
        // TODO: push notification to customer
    }
}
