package org.example.orderservice.event;

import org.example.orderservice.BaseIntegrationTest;
import org.example.orderservice.dto.CreateOrderRequest;
import org.example.orderservice.dto.UpdateOrderStatusRequest;
import org.example.orderservice.model.Customer;
import org.example.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderEventListenerIntegrationTest extends BaseIntegrationTest {

    @MockitoSpyBean
    private OrderEventListener orderEventListener;

    @Test
    void should_fireOnOrderCreated_when_orderSavedSuccessfully() throws Exception {
        Customer owner = createTestCustomer();
        String token = generateTokenFor(owner);

        createOrderViaApi(buildCreateOrderRequest(owner.getId()), token);

        verify(orderEventListener, times(1)).onOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void should_notFireOnOrderCreated_when_transactionRollsBack() throws Exception {
        // Admin bypasses ownership check, so we actually enter the transaction.
        // A non-existent customerId causes EntityNotFoundException inside the TX → rollback.
        Customer admin = createTestCustomerWithRole(org.example.orderservice.model.CustomerRole.ROLE_ADMIN);
        String adminToken = generateTokenFor(admin);

        CreateOrderRequest badRequest = buildCreateOrderRequest(UUID.randomUUID());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isNotFound());

        verify(orderEventListener, never()).onOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void should_fireOnOrderStatusChanged_when_statusUpdatedSuccessfully() throws Exception {
        Customer owner = createTestCustomer();
        String ownerToken = generateTokenFor(owner);
        String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), ownerToken);

        // Status updates are admin-only
        Customer admin = createTestCustomerWithRole(org.example.orderservice.model.CustomerRole.ROLE_ADMIN);
        String adminToken = generateTokenFor(admin);

        mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk());

        verify(orderEventListener, times(1)).onOrderStatusChanged(any(OrderStatusChangedEvent.class));
    }
}
