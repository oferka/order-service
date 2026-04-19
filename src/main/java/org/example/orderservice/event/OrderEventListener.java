package org.example.orderservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private static final String MDC_CORRELATION_ID = "correlationId";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            if (event.correlationId() != null) MDC.put(MDC_CORRELATION_ID, event.correlationId());
            log.info("Order created: orderId={}, orderNumber={}, customerId={}, totalAmount={}, timestamp={}",
                    event.orderId(), event.orderNumber(), event.customerId(), event.totalAmount(), event.timestamp());
            // TODO: send order confirmation email
            // TODO: notify inventory service
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            if (event.correlationId() != null) MDC.put(MDC_CORRELATION_ID, event.correlationId());
            log.info("Order status changed: orderId={}, orderNumber={}, {} -> {}, timestamp={}",
                    event.orderId(), event.orderNumber(), event.previousStatus(), event.newStatus(), event.timestamp());
            // TODO: push notification to customer
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
        }
    }
}
