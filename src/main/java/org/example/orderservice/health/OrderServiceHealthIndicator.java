package org.example.orderservice.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class OrderServiceHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public OrderServiceHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1");
            return Health.up()
                    .withDetail("database", "reachable")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "unreachable")
                    .withException(e)
                    .build();
        }
    }
}
