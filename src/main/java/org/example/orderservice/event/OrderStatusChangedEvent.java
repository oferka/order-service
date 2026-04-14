package org.example.orderservice.event;

import org.example.orderservice.model.Order;
import org.example.orderservice.model.OrderStatus;
import org.springframework.context.ApplicationEvent;

public class OrderStatusChangedEvent extends ApplicationEvent {

    private final Order order;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        super(order);
        this.order = order;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public Order getOrder() {
        return order;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }
}
