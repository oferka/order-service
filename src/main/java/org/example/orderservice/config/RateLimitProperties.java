package org.example.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "rate-limit.auth")
public class RateLimitProperties {

    private int capacity = 10;
    private int refillTokens = 10;
    private int refillPeriodMinutes = 1;
    private List<String> trustedProxies = List.of("127.0.0.1", "::1");
    private int cacheMaxSize = 100_000;
    private int cacheTtlMinutes = 5;

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getRefillTokens() { return refillTokens; }
    public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }

    public int getRefillPeriodMinutes() { return refillPeriodMinutes; }
    public void setRefillPeriodMinutes(int refillPeriodMinutes) { this.refillPeriodMinutes = refillPeriodMinutes; }

    public List<String> getTrustedProxies() { return trustedProxies; }
    public void setTrustedProxies(List<String> trustedProxies) { this.trustedProxies = trustedProxies; }

    public int getCacheMaxSize() { return cacheMaxSize; }
    public void setCacheMaxSize(int cacheMaxSize) { this.cacheMaxSize = cacheMaxSize; }

    public int getCacheTtlMinutes() { return cacheTtlMinutes; }
    public void setCacheTtlMinutes(int cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }
}
