package org.example.orderservice.service;

import org.example.orderservice.dto.CreateOrderRequest;
import org.example.orderservice.dto.OrderResponse;
import org.example.orderservice.dto.PagedResponse;
import org.example.orderservice.dto.UpdateOrderStatusRequest;
import org.example.orderservice.model.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    PagedResponse<OrderResponse> listOrders(UUID customerId, OrderStatus status, Pageable pageable);

    OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request);

    void cancelOrder(String orderNumber);
}
