package org.example.orderservice;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceApplicationTests.class);

    @Test
    void contextLoads() {
        log.info("Application context loaded successfully");
    }
}
