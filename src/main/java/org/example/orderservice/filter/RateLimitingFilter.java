package org.example.orderservice.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String AUTH_TOKEN_PATH = "/api/v1/auth/token";

    // Matches IPv4 (e.g. 192.168.1.1) and IPv6 (e.g. ::1, 2001:db8::1) literals only.
    // Rejects hostnames and arbitrary strings so they cannot be used as bucket keys.
    private static final Pattern IP_PATTERN =
            Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$|^[0-9a-fA-F:]+$");

    private final Set<String> trustedProxies;
    private final Cache<String, Bucket> buckets;
    private final RateLimitProperties properties;

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
        this.trustedProxies = Set.copyOf(properties.getTrustedProxies());
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.getCacheMaxSize())
                .expireAfterAccess(properties.getCacheTtlMinutes(), TimeUnit.MINUTES)
                .build();
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
        Bucket bucket = buckets.get(clientIp, ip -> createBucket());
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
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddr;
        }
        String candidate = forwarded.split(",")[0].trim();
        if (!isValidIp(candidate)) {
            log.warn("Invalid IP in X-Forwarded-For header: '{}', falling back to remoteAddr={}", candidate, remoteAddr);
            return remoteAddr;
        }
        return candidate;
    }

    private static boolean isValidIp(String candidate) {
        return candidate != null && IP_PATTERN.matcher(candidate).matches();
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                properties.getCapacity(),
                Refill.intervally(properties.getRefillTokens(),
                        Duration.ofMinutes(properties.getRefillPeriodMinutes())));
        return Bucket.builder().addLimit(limit).build();
    }
}
