package org.example.orderservice.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTransitionTest {

    @ParameterizedTest(name = "{0} → {1} should succeed")
    @MethodSource("validTransitions")
    void transitionTo_validTransition_updatesStatus(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);

        order.transitionTo(to);

        assertThat(order.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0} → {1} should throw IllegalStateException")
    @MethodSource("invalidTransitions")
    void transitionTo_invalidTransition_throwsIllegalStateException(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);

        assertThatThrownBy(() -> order.transitionTo(to))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition order from " + from + " to " + to);
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(OrderStatus.CREATED,   OrderStatus.CONFIRMED),
                Arguments.of(OrderStatus.CREATED,   OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.SHIPPED,   OrderStatus.DELIVERED)
        );
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                // From CREATED
                Arguments.of(OrderStatus.CREATED,   OrderStatus.SHIPPED),
                Arguments.of(OrderStatus.CREATED,   OrderStatus.DELIVERED),
                // From CONFIRMED
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CREATED),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERED),
                // From SHIPPED
                Arguments.of(OrderStatus.SHIPPED,   OrderStatus.CREATED),
                Arguments.of(OrderStatus.SHIPPED,   OrderStatus.CONFIRMED),
                Arguments.of(OrderStatus.SHIPPED,   OrderStatus.CANCELLED),
                // From DELIVERED (terminal)
                Arguments.of(OrderStatus.DELIVERED, OrderStatus.CREATED),
                Arguments.of(OrderStatus.DELIVERED, OrderStatus.CONFIRMED),
                Arguments.of(OrderStatus.DELIVERED, OrderStatus.SHIPPED),
                Arguments.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
                // From CANCELLED (terminal)
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.CREATED),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.CONFIRMED),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.SHIPPED),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED)
        );
    }

    private Order orderWithStatus(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-TEST0001")
                .status(status)
                .orderItems(new ArrayList<>())
                .build();
    }
}
