package org.example.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limit.auth")
public class RateLimitProperties {

    private int capacity = 10;
    private int refillTokens = 10;
    private int refillPeriodMinutes = 1;

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getRefillTokens() { return refillTokens; }
    public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }

    public int getRefillPeriodMinutes() { return refillPeriodMinutes; }
    public void setRefillPeriodMinutes(int refillPeriodMinutes) { this.refillPeriodMinutes = refillPeriodMinutes; }
}
