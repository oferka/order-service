package org.example.orderservice;

import tools.jackson.databind.ObjectMapper;
import org.example.orderservice.dto.AddressRequest;
import org.example.orderservice.dto.CreateOrderRequest;
import org.example.orderservice.dto.OrderItemRequest;
import org.example.orderservice.model.Customer;
import org.example.orderservice.repository.CustomerRepository;
import org.example.orderservice.repository.OrderItemRepository;
import org.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@ActiveProfiles("test")
@WithMockUser
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private WebApplicationContext context;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected CustomerRepository customerRepository;
    @Autowired protected OrderRepository orderRepository;
    @Autowired protected OrderItemRepository orderItemRepository;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
    }

    protected Customer createTestCustomer() {
        return customerRepository.save(Customer.builder()
                .email("test-" + UUID.randomUUID() + "@example.com")
                .fullName("Test Customer")
                .build());
    }

    protected CreateOrderRequest buildCreateOrderRequest(UUID customerId) {
        return new CreateOrderRequest(
                customerId,
                List.of(new OrderItemRequest("prod-1", "Test Widget", 2, new BigDecimal("15.00"))),
                new AddressRequest("123 Main St", "Springfield", "IL", "62701", "US")
        );
    }

    protected String createOrderViaApi(CreateOrderRequest request) throws Exception {
        String body = mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("orderNumber").asText();
    }
}
