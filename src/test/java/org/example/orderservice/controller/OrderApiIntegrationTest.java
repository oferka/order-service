package org.example.orderservice.controller;

import org.example.orderservice.BaseIntegrationTest;
import org.example.orderservice.dto.CreateOrderRequest;
import org.example.orderservice.dto.UpdateOrderStatusRequest;
import org.example.orderservice.model.Customer;
import org.example.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class OrderApiIntegrationTest extends BaseIntegrationTest {

    @Nested
    class CreateOrder {

        @Test
        void should_return201WithLocation_when_orderRequestIsValid() throws Exception {
            Customer customer = createTestCustomer();

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateOrderRequest(customer.getId()))))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.orderNumber").exists())
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.customerEmail").value(customer.getEmail()));
        }

        @Test
        void should_return400_when_orderRequestIsInvalid() throws Exception {
            Customer customer = createTestCustomer();
            CreateOrderRequest invalidRequest = new CreateOrderRequest(
                    customer.getId(), List.of(), null
            );

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

    @Nested
    class GetOrder {

        @Test
        void should_return200_when_orderExists() throws Exception {
            Customer customer = createTestCustomer();
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(customer.getId()));

            mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.customerEmail").value(customer.getEmail()))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(1));
        }

        @Test
        void should_return404_when_orderNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/orders/ORD-NOTEXIST"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }

    @Nested
    class UpdateOrderStatus {

        @Test
        void should_return200_when_statusTransitionIsValid() throws Exception {
            Customer customer = createTestCustomer();
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(customer.getId()));

            mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CONFIRMED))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        void should_return409_when_statusTransitionIsInvalid() throws Exception {
            Customer customer = createTestCustomer();
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(customer.getId()));

            mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Conflict"));
        }
    }

    @Nested
    class ListOrders {

        @Test
        void should_returnPagedOrders_when_paginationIsRequested() throws Exception {
            Customer customer = createTestCustomer();
            CreateOrderRequest request = buildCreateOrderRequest(customer.getId());
            for (int i = 0; i < 15; i++) {
                createOrderViaApi(request);
            }

            mockMvc.perform(get("/api/v1/orders")
                            .param("customerId", customer.getId().toString())
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }
    }
}
