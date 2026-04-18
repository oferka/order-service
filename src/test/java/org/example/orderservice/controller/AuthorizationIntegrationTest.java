package org.example.orderservice.controller;

import org.example.orderservice.BaseIntegrationTest;
import org.example.orderservice.dto.UpdateOrderStatusRequest;
import org.example.orderservice.model.Customer;
import org.example.orderservice.model.CustomerRole;
import org.example.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationIntegrationTest extends BaseIntegrationTest {

    // -------------------------------------------------------------------------
    // GET /api/v1/orders/{orderNumber}
    // -------------------------------------------------------------------------
    @Nested
    class GetOrder {

        @Test
        void should_return200_when_ownerRequests() throws Exception {
            Customer owner = createTestCustomer();
            String token = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), token);

            mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value(orderNumber));
        }

        @Test
        void should_return403_when_nonOwnerRequests() throws Exception {
            Customer owner = createTestCustomer();
            String ownerToken = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), ownerToken);

            Customer other = createTestCustomer();
            String otherToken = generateTokenFor(other);

            mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_return200_when_adminRequests() throws Exception {
            Customer owner = createTestCustomer();
            String ownerToken = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), ownerToken);

            Customer admin = createTestCustomerWithRole(CustomerRole.ROLE_ADMIN);
            String adminToken = generateTokenFor(admin);

            mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void should_return401_when_unauthenticated() throws Exception {
            Customer owner = createTestCustomer();
            String ownerToken = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), ownerToken);

            mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/orders
    // -------------------------------------------------------------------------
    @Nested
    class ListOrders {

        @Test
        void should_returnOnlyOwnOrders_when_regularUserRequests() throws Exception {
            Customer userA = createTestCustomer();
            String tokenA = generateTokenFor(userA);
            createOrderViaApi(buildCreateOrderRequest(userA.getId()), tokenA);
            createOrderViaApi(buildCreateOrderRequest(userA.getId()), tokenA);

            Customer userB = createTestCustomer();
            String tokenB = generateTokenFor(userB);
            createOrderViaApi(buildCreateOrderRequest(userB.getId()), tokenB);

            // User A should see only their 2 orders, even without passing customerId
            mockMvc.perform(get("/api/v1/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void should_ignoreCustomerIdFilter_when_regularUserPassesAnotherUsersId() throws Exception {
            Customer userA = createTestCustomer();
            String tokenA = generateTokenFor(userA);
            createOrderViaApi(buildCreateOrderRequest(userA.getId()), tokenA);

            Customer userB = createTestCustomer();
            String tokenB = generateTokenFor(userB);
            createOrderViaApi(buildCreateOrderRequest(userB.getId()), tokenB);

            // User A passing User B's customerId is silently overridden — still sees only their own 1 order
            mockMvc.perform(get("/api/v1/orders")
                            .param("customerId", userB.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void should_returnAllOrders_when_adminRequests() throws Exception {
            Customer userA = createTestCustomer();
            String tokenA = generateTokenFor(userA);
            createOrderViaApi(buildCreateOrderRequest(userA.getId()), tokenA);

            Customer userB = createTestCustomer();
            String tokenB = generateTokenFor(userB);
            createOrderViaApi(buildCreateOrderRequest(userB.getId()), tokenB);

            Customer admin = createTestCustomerWithRole(CustomerRole.ROLE_ADMIN);
            String adminToken = generateTokenFor(admin);

            mockMvc.perform(get("/api/v1/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/orders/{orderNumber}/status  — admin only
    // -------------------------------------------------------------------------
    @Nested
    class UpdateOrderStatus {

        @Test
        void should_return403_when_regularUserUpdatesStatus() throws Exception {
            Customer owner = createTestCustomer();
            String token = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), token);

            mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateOrderStatusRequest(OrderStatus.CONFIRMED))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_return200_when_adminUpdatesStatus() throws Exception {
            Customer owner = createTestCustomer();
            String ownerToken = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), ownerToken);

            Customer admin = createTestCustomerWithRole(CustomerRole.ROLE_ADMIN);
            String adminToken = generateTokenFor(admin);

            mockMvc.perform(patch("/api/v1/orders/{orderNumber}/status", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateOrderStatusRequest(OrderStatus.CONFIRMED))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/orders/{orderNumber}  — owner or admin
    // -------------------------------------------------------------------------
    @Nested
    class CancelOrder {

        @Test
        void should_return204_when_ownerCancels() throws Exception {
            Customer owner = createTestCustomer();
            String token = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), token);

            mockMvc.perform(delete("/api/v1/orders/{orderNumber}", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        void should_return403_when_nonOwnerCancels() throws Exception {
            Customer owner = createTestCustomer();
            String ownerToken = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), ownerToken);

            Customer other = createTestCustomer();
            String otherToken = generateTokenFor(other);

            mockMvc.perform(delete("/api/v1/orders/{orderNumber}", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_return204_when_adminCancels() throws Exception {
            Customer owner = createTestCustomer();
            String ownerToken = generateTokenFor(owner);
            String orderNumber = createOrderViaApi(buildCreateOrderRequest(owner.getId()), ownerToken);

            Customer admin = createTestCustomerWithRole(CustomerRole.ROLE_ADMIN);
            String adminToken = generateTokenFor(admin);

            mockMvc.perform(delete("/api/v1/orders/{orderNumber}", orderNumber)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/orders  — creator must match authenticated user
    // -------------------------------------------------------------------------
    @Nested
    class CreateOrder {

        @Test
        void should_return403_when_userCreatesOrderForAnotherCustomer() throws Exception {
            Customer realOwner = createTestCustomer();
            Customer attacker = createTestCustomer();
            String attackerToken = generateTokenFor(attacker);

            mockMvc.perform(post("/api/v1/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + attackerToken)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildCreateOrderRequest(realOwner.getId()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_return201_when_adminCreatesOrderForAnyCustomer() throws Exception {
            Customer owner = createTestCustomer();
            Customer admin = createTestCustomerWithRole(CustomerRole.ROLE_ADMIN);
            String adminToken = generateTokenFor(admin);

            mockMvc.perform(post("/api/v1/orders")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildCreateOrderRequest(owner.getId()))))
                    .andExpect(status().isCreated());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/customers/{id}  — own profile or admin
    // -------------------------------------------------------------------------
    @Nested
    class GetCustomer {

        @Test
        void should_return200_when_userRequestsOwnProfile() throws Exception {
            Customer customer = createTestCustomer();
            String token = generateTokenFor(customer);

            mockMvc.perform(get("/api/v1/customers/{id}", customer.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(customer.getId().toString()));
        }

        @Test
        void should_return403_when_userRequestsAnotherProfile() throws Exception {
            Customer customerA = createTestCustomer();
            Customer customerB = createTestCustomer();
            String tokenB = generateTokenFor(customerB);

            mockMvc.perform(get("/api/v1/customers/{id}", customerA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_return200_when_adminRequestsAnyProfile() throws Exception {
            Customer customer = createTestCustomer();
            Customer admin = createTestCustomerWithRole(CustomerRole.ROLE_ADMIN);
            String adminToken = generateTokenFor(admin);

            mockMvc.perform(get("/api/v1/customers/{id}", customer.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/customers/by-email  — admin only
    // -------------------------------------------------------------------------
    @Nested
    class GetCustomerByEmail {

        @Test
        void should_return403_when_regularUserSearchesByEmail() throws Exception {
            Customer customer = createTestCustomer();
            String token = generateTokenFor(customer);

            mockMvc.perform(get("/api/v1/customers/by-email")
                            .param("email", customer.getEmail())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_return200_when_adminSearchesByEmail() throws Exception {
            Customer customer = createTestCustomer();
            Customer admin = createTestCustomerWithRole(CustomerRole.ROLE_ADMIN);
            String adminToken = generateTokenFor(admin);

            mockMvc.perform(get("/api/v1/customers/by-email")
                            .param("email", customer.getEmail())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(customer.getEmail()));
        }
    }
}
