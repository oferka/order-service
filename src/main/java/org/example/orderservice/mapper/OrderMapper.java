package org.example.orderservice.mapper;

import org.example.orderservice.dto.AddressRequest;
import org.example.orderservice.dto.CreateOrderRequest;
import org.example.orderservice.dto.OrderItemRequest;
import org.example.orderservice.dto.OrderItemResponse;
import org.example.orderservice.dto.OrderResponse;
import org.example.orderservice.model.Order;
import org.example.orderservice.model.OrderItem;
import org.example.orderservice.model.ShippingAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "orderItems", source = "items")
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    OrderItem toOrderItem(OrderItemRequest request);

    ShippingAddress toShippingAddress(AddressRequest request);

    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "items", source = "orderItems")
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem orderItem);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> orderItems);
}
