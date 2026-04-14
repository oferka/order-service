package org.example.orderservice.service;

import org.example.orderservice.dto.AddressRequest;
import org.example.orderservice.dto.CreateOrderRequest;
import org.example.orderservice.dto.OrderItemRequest;
import org.example.orderservice.dto.OrderResponse;
import org.example.orderservice.dto.PagedResponse;
import org.example.orderservice.dto.UpdateOrderStatusRequest;
import org.example.orderservice.event.OrderCreatedEvent;
import org.example.orderservice.event.OrderStatusChangedEvent;
import org.example.orderservice.exception.EntityNotFoundException;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.model.Customer;
import org.example.orderservice.model.Order;
import org.example.orderservice.model.OrderStatus;
import org.example.orderservice.repository.CustomerRepository;
import org.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_Success() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        AddressRequest address = new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US");
        OrderItemRequest item = new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("15.00"));
        CreateOrderRequest request = new CreateOrderRequest(customerId, List.of(item), address);

        Customer customer = Customer.builder()
                .id(customerId).email("jane@example.com").fullName("Jane Doe").build();
        Order mappedOrder = Order.builder().orderItems(new ArrayList<>()).build();
        Order savedOrder = Order.builder()
                .id(orderId).orderNumber("ORD-TEST0001").customer(customer)
                .status(OrderStatus.CREATED).totalAmount(new BigDecimal("30.00"))
                .orderItems(new ArrayList<>()).build();
        OrderResponse expectedResponse = new OrderResponse(orderId, "ORD-TEST0001", OrderStatus.CREATED,
                "jane@example.com", "Jane Doe", List.of(), null, new BigDecimal("30.00"), null, null);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(mappedOrder.getCustomer()).isEqualTo(customer);
        assertThat(mappedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(mappedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        verify(orderRepository, times(1)).save(mappedOrder);
        verify(eventPublisher, times(1)).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_CustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(customerId, List.of(),
                new AddressRequest("s", "c", "st", "z", "US"));

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(EntityNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateOrderStatus_ValidTransition() {
        String orderNumber = "ORD-VALID001";
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId).orderNumber(orderNumber)
                .status(OrderStatus.CREATED).orderItems(new ArrayList<>()).build();
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        Order savedOrder = Order.builder()
                .id(orderId).orderNumber(orderNumber)
                .status(OrderStatus.CONFIRMED).orderItems(new ArrayList<>()).build();
        OrderResponse expectedResponse = new OrderResponse(orderId, orderNumber, OrderStatus.CONFIRMED,
                null, null, List.of(), null, BigDecimal.ZERO, null, null);

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        OrderResponse result = orderService.updateOrderStatus(orderNumber, request);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, times(1)).save(order);
        verify(eventPublisher, times(1)).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateOrderStatus_InvalidTransition() {
        String orderNumber = "ORD-DELIV001";
        Order order = Order.builder()
                .id(UUID.randomUUID()).orderNumber(orderNumber)
                .status(OrderStatus.DELIVERED).orderItems(new ArrayList<>()).build();
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CREATED);

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderNumber, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cancelOrder_Success() {
        String orderNumber = "ORD-CANCEL01";
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId).orderNumber(orderNumber)
                .status(OrderStatus.CREATED).orderItems(new ArrayList<>()).build();
        Order savedOrder = Order.builder()
                .id(orderId).orderNumber(orderNumber)
                .status(OrderStatus.CANCELLED).orderItems(new ArrayList<>()).build();
        OrderResponse expectedResponse = new OrderResponse(orderId, orderNumber, OrderStatus.CANCELLED,
                null, null, List.of(), null, BigDecimal.ZERO, null, null);

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        orderService.cancelOrder(orderNumber);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, times(1)).save(order);
        verify(eventPublisher, times(1)).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void cancelOrder_AlreadyDelivered() {
        String orderNumber = "ORD-DELIV002";
        Order order = Order.builder()
                .id(UUID.randomUUID()).orderNumber(orderNumber)
                .status(OrderStatus.DELIVERED).orderItems(new ArrayList<>()).build();

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void listOrders_WithFilters() {
        UUID customerId = UUID.randomUUID();
        OrderStatus status = OrderStatus.CONFIRMED;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        PagedResponse<OrderResponse> result = orderService.listOrders(customerId, status, pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.totalPages()).isEqualTo(0);
        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }
}
