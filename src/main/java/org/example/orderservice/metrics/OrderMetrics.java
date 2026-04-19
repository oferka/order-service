package org.example.orderservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.orderservice.model.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMetrics {

    private static final String ORDERS_CREATED_TOTAL = "orders.created.total";
    private static final String ORDERS_STATUS_CHANGED_TOTAL = "orders.status.changed.total";
    private static final String ORDERS_CREATION_DURATION = "orders.creation.duration";
    private static final String ORDERS_ACTIVE_COUNT = "orders.active.count";

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED, OrderStatus.SHIPPED);

    private final MeterRegistry registry;
    private final Counter ordersCreatedCounter;
    private final Timer creationTimer;

    public OrderMetrics(MeterRegistry registry, OrderRepository orderRepository) {
        this.registry = registry;

        this.ordersCreatedCounter = Counter.builder(ORDERS_CREATED_TOTAL)
                .description("Total number of orders created")
                .register(registry);

        this.creationTimer = Timer.builder(ORDERS_CREATION_DURATION)
                .description("Time taken to create an order")
                .register(registry);

        Gauge.builder(ORDERS_ACTIVE_COUNT, orderRepository,
                        repo -> repo.countByStatusIn(ACTIVE_STATUSES))
                .description("Number of active (non-terminal) orders")
                .register(registry);
    }

    public void recordOrderCreated() {
        ordersCreatedCounter.increment();
    }

    public void recordStatusChanged(OrderStatus fromStatus, OrderStatus toStatus) {
        registry.counter(ORDERS_STATUS_CHANGED_TOTAL,
                        "from_status", fromStatus.name(),
                        "to_status", toStatus.name())
                .increment();
    }

    public Timer getCreationTimer() {
        return creationTimer;
    }
}
