package org.example.orderservice.service;

import org.example.orderservice.dto.CreateOrderRequest;
import org.example.orderservice.dto.OrderResponse;
import org.example.orderservice.dto.PagedResponse;
import org.example.orderservice.dto.UpdateOrderStatusRequest;
import org.example.orderservice.event.OrderCreatedEvent;
import org.example.orderservice.event.OrderStatusChangedEvent;
import org.example.orderservice.exception.EntityNotFoundException;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.metrics.OrderMetrics;
import org.example.orderservice.model.Customer;
import org.example.orderservice.model.Order;
import org.example.orderservice.model.OrderStatus;
import org.example.orderservice.repository.CustomerRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMetrics orderMetrics;
    private final SecurityUtils securityUtils;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            OrderMapper orderMapper,
                            ApplicationEventPublisher eventPublisher,
                            OrderMetrics orderMetrics,
                            SecurityUtils securityUtils) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
        this.orderMetrics = orderMetrics;
        this.securityUtils = securityUtils;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (!securityUtils.isAdmin() && !request.customerId().equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        return orderMetrics.getCreationTimer().record(() -> {
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
            orderMetrics.recordOrderCreated();

            return orderMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findWithItemsByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
        assertOwnerOrAdmin(order.getCustomer().getId());
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrders(UUID customerId, OrderStatus status, Pageable pageable) {
        // Non-admins always see only their own orders regardless of the filter passed
        UUID effectiveCustomerId = securityUtils.isAdmin() ? customerId : securityUtils.getCurrentUserId();

        List<Specification<Order>> specs = new ArrayList<>();
        if (effectiveCustomerId != null) {
            UUID id = effectiveCustomerId;
            specs.add((root, query, cb) -> cb.equal(root.get("customer").get("id"), id));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Order> page = orderRepository.findAll(Specification.allOf(specs), pageable);

        List<UUID> ids = page.getContent().stream().map(Order::getId).toList();
        Map<UUID, Order> byId = orderRepository.findAllWithItemsAndCustomerByIdIn(ids).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        List<OrderResponse> content = ids.stream()
                .map(id -> orderMapper.toResponse(byId.get(id)))
                .toList();

        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));

        OrderStatus previousStatus = order.getStatus();
        order.transitionTo(request.status());
        Order saved = orderRepository.save(order);

        log.info("Order status changed: orderNumber={}, {} -> {}", orderNumber, previousStatus, request.status());
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                saved.getId(), saved.getOrderNumber(), previousStatus, request.status(), Instant.now()));
        orderMetrics.recordStatusChanged(previousStatus, request.status());

        return orderMapper.toResponse(saved);
    }

    @Override
    public void cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderNumber));
        assertOwnerOrAdmin(order.getCustomer().getId());
        // Calls updateOrderStatus via this (bypasses @PreAuthorize proxy — intentional,
        // cancelOrder has its own ownership check above).
        updateOrderStatus(orderNumber, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
    }

    private void assertOwnerOrAdmin(UUID ownerId) {
        if (!securityUtils.isAdmin() && !ownerId.equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
