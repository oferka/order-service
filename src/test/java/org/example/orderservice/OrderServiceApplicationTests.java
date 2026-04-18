package org.example.orderservice;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OrderServiceApplicationTests extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceApplicationTests.class);

    @Test
    void contextLoads() {
        log.info("Application context loaded successfully");
    }
}
