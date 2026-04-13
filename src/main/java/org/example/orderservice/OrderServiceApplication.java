package org.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class OrderServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
