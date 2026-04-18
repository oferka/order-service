package org.example.orderservice.filter;

import org.example.orderservice.BaseIntegrationTest;
import org.example.orderservice.dto.TokenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "rate-limit.auth.capacity=2")
class RateLimitingFilterIntegrationTest extends BaseIntegrationTest {

    private static final String AUTH_PATH = "/api/v1/auth/token";

    @Test
    void should_return429_when_rateLimitExceeded() throws Exception {
        // Use a unique IP so this test doesn't share bucket state with others
        String ip = "10.1.1.1";
        String body = objectMapper.writeValueAsString(new TokenRequest("x@example.com", "wrongpassword"));

        // First 2 requests pass through the rate limiter (get 401 for bad creds, not 429)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post(AUTH_PATH)
                            .header("X-Forwarded-For", ip)
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(result ->
                            assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
        }

        // 3rd request — bucket empty, must be rejected with 429
        mockMvc.perform(post(AUTH_PATH)
                        .header("X-Forwarded-For", ip)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    void should_trackBucketsSeparately_perClientIp() throws Exception {
        String body = objectMapper.writeValueAsString(new TokenRequest("x@example.com", "wrongpassword"));

        // Two different IPs should each get their own bucket (capacity=2 each)
        for (String ip : new String[]{"10.2.2.1", "10.2.2.2"}) {
            for (int i = 0; i < 2; i++) {
                mockMvc.perform(post(AUTH_PATH)
                                .header("X-Forwarded-For", ip)
                                .contentType(APPLICATION_JSON)
                                .content(body))
                        .andExpect(result ->
                                assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
            }
        }
    }

    @Test
    void should_fallbackToRemoteAddr_when_xForwardedForIsInvalidIp() throws Exception {
        // remoteAddr is 127.0.0.1 (trusted proxy in tests); an invalid X-Forwarded-For
        // must be rejected and the bucket keyed on remoteAddr instead — no 500 or bypass.
        String body = objectMapper.writeValueAsString(new TokenRequest("x@example.com", "wrongpassword"));

        mockMvc.perform(post(AUTH_PATH)
                        .header("X-Forwarded-For", "not-an-ip-address")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(500));
    }

    @Test
    void should_notApplyRateLimit_toOtherEndpoints() throws Exception {
        String ip = "10.3.3.1";

        // More requests than the auth capacity — other endpoints must never see 429
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/orders")
                            .header("X-Forwarded-For", ip))
                    .andExpect(result ->
                            assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
        }
    }
}
