package org.example.orderservice.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.orderservice.config.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String AUTH_TOKEN_PATH = "/api/v1/auth/token";

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isAuthTokenRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> createBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
        log.warn("Rate limit exceeded for IP={}, retryAfter={}s", clientIp, retryAfterSeconds);

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Retry after "
                + retryAfterSeconds + " seconds.\",\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private boolean isAuthTokenRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && AUTH_TOKEN_PATH.equals(request.getRequestURI());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                properties.getCapacity(),
                Refill.intervally(properties.getRefillTokens(),
                        Duration.ofMinutes(properties.getRefillPeriodMinutes())));
        return Bucket.builder().addLimit(limit).build();
    }
}
