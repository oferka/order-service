package org.example.orderservice.service;

import org.example.orderservice.dto.CreateOrderRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            OrderMapper orderMapper,
                            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", request.customerId()));

        Order order = orderMapper.toEntity(request);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.getOrderItems().forEach(item -> item.setOrder(order));

        BigDecimal totalAmount = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        Order saved = orderRepository.save(order);
        log.info("Order created: orderNumber={}, customerId={}", saved.getOrderNumber(), customer.getId());
        eventPublisher.publishEvent(new OrderCreatedEvent(
                saved.getId(), saved.getOrderNumber(), customer.getId(), saved.getTotalAmount(), Instant.now()));

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findWithItemsByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrders(UUID customerId, OrderStatus status, Pageable pageable) {
        Specification<Order> spec = Specification.where(null);

        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Order> page = orderRepository.findAll(spec, pageable);
        List<OrderResponse> content = page.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();

        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));

        OrderStatus previousStatus = order.getStatus();
        order.transitionTo(request.status());
        Order saved = orderRepository.save(order);

        log.info("Order status changed: orderNumber={}, {} -> {}", orderNumber, previousStatus, request.status());
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                saved.getId(), saved.getOrderNumber(), previousStatus, request.status(), Instant.now()));

        return orderMapper.toResponse(saved);
    }

    @Override
    public void cancelOrder(String orderNumber) {
        updateOrderStatus(orderNumber, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
    }
}
