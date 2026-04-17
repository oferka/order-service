package org.example.orderservice.controller;

import org.example.orderservice.BaseIntegrationTest;
import org.example.orderservice.dto.CreateCustomerRequest;
import org.example.orderservice.dto.TokenRequest;
import org.example.orderservice.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void should_accessOrdersEndpoint_when_fullAuthFlowCompleted() throws Exception {

        // Step 1 — Create a customer (no auth required)
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCustomerRequest("alice@example.com", "Alice Smith", "+1234567890"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        // Step 2 — Obtain a JWT token (no auth required)
        String tokenResponseJson = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRequest("alice@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readValue(tokenResponseJson, TokenResponse.class).token();

        // Step 3 — Access a protected orders endpoint using the token
        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void should_return401_when_noTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return401_when_invalidTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.valid"))
                .andExpect(status().isUnauthorized());
    }
}
