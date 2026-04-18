I have the full picture. A few corrections to note before presenting:
- The tools.jackson import is correct for Spring Boot 4 / Jackson 3 — not an issue.
- The "SQL injection in IP extraction" label is wrong — it's not SQL injection; the correct concern is rate limit bypass via X-Forwarded-For header spoofing (real but Medium severity, not Critical).
- The JWT filter "silent continue" is correct Spring Security behavior — an unauthenticated request still hits anyRequest().authenticated() and gets 401 via the entry point. The real gap is DEBUG-level logging making attack patterns invisible.
- No application-prod.yml exists — the actuator finding referenced a nonexistent file.
- The two-query listOrders is the intentional N+1 fix — not a new N+1 problem.

  ---                                                                                                                                                                                                                                                                                                          
Executive Summary

- The service has strong architectural fundamentals: clean layering, constructor injection throughout, solid DTO boundaries, and good test infrastructure.
- One Critical security issue demands immediate attention: the JWT signing secret is hardcoded in version-controlled application.yml — anyone with repo access can forge tokens for any user or role.
- A silent memory leak is already in production: Counter.builder().register() is called on every request, creating a new time-series per unique customer ID. Under sustained load this will bloat the metrics registry.
- Test coverage has a meaningful gap: CustomerServiceImpl — the most security-sensitive class (password hashing, credential verification, ownership checks) — has zero unit tests.
- The application-test.yml has H2 config that is completely overridden by @DynamicPropertySource and never used; it silently misleads developers into thinking tests run against H2.
- OrderServiceApplication.main is missing public, and no application-prod.yml exists — actuator exposure in production is uncontrolled.

  ---                                                                                                                                                                                                                                                                                                          
Security

[CRITICAL] Hardcoded JWT signing secret in source control

- Location: src/main/resources/application.yml:67
- Description: order-service-jwt-signing-secret-key-for-hmac-sha256-algorithm is committed in plaintext.
- Impact: Anyone with repo access can sign valid JWTs for any user ID and any role, bypassing all authentication and authorization.
- Recommendation: Replace with ${JWT_SECRET} and inject via environment variable. Rotate immediately in all environments.

[HIGH] JWT signing algorithm not pinned — algorithm confusion possible

- Location: src/main/java/org/example/orderservice/security/JwtService.java
- Description: If the validation path uses Jwts.parser() without explicitly asserting requireAlgorithm(SignatureAlgorithm.HS256) (or the JJWT 0.12 equivalent), a crafted token with "alg": "none" could bypass signature verification on some JJWT versions.
- Impact: Authentication bypass via algorithm downgrade.
- Recommendation: Verify the parser is built with .verifyWith(key) and that JJWT 0.12's parserBuilder() API is used correctly — JJWT 0.12 does reject none by default, but confirm explicitly.

[MEDIUM] Rate limit bypass via X-Forwarded-For header spoofing

- Location: src/main/java/org/example/orderservice/filter/RateLimitingFilter.java:60–63
- Description: The filter trusts the client-supplied X-Forwarded-For header as the rate-limit key without validating it. A client connecting directly (not through a trusted reverse proxy) can rotate fake IPs to get unlimited auth attempts. Additionally, unique forged IPs grow the ConcurrentHashMap
  unboundedly — a memory exhaustion vector.
- Impact: Brute-force protection is bypassed; potential OOM under attack.
- Recommendation: Only trust X-Forwarded-For when the request originates from a known trusted proxy IP. Validate the extracted value is a legal IP address before using it as a map key. Add a max-size cap to the ConcurrentHashMap (or replace with Caffeine cache with TTL and maximumSize).

[MEDIUM] JWT validation failures invisible in production

- Location: src/main/java/org/example/orderservice/filter/JwtAuthenticationFilter.java:55–57
- Description: All JWT exceptions (expired, tampered, malformed) are caught and logged only at DEBUG level. Production logging is typically INFO or higher, so attacks using invalid tokens generate zero log noise.
- Impact: Brute-force token-forging or replay attacks are completely invisible in production logs.
- Recommendation: Log at WARN level. Include the failure reason (without the token value): log.warn("JWT validation failed for request {} {}: {}", method, uri, e.getMessage()).

[MEDIUM] Email enumeration via duplicate registration error

- Location: src/main/java/org/example/orderservice/service/CustomerServiceImpl.java
- Description: Error message for duplicate email registration reveals whether an address is registered.
- Impact: Attackers can cheaply enumerate valid user emails.
- Recommendation: Return a generic message: "Registration failed".

  ---                                                                                                                                                                                                                                                                                                        
Performance

[HIGH] Missing index on orders.status

- Location: src/main/resources/db/migration/V1__create_order_schema.sql
- Description: listOrders filters by status via Specification; countByStatusIn (used by the active-orders Gauge) also queries by status. No index exists on the column.
- Impact: Full table scan on every list and metrics poll. Grows linearly with order volume.
- Recommendation: New migration: CREATE INDEX idx_orders_status ON orders (status);

[HIGH] Missing index on order_items.order_id

- Location: src/main/resources/db/migration/V1__create_order_schema.sql
- Description: The FK column order_id in order_items has no index. findAllWithItemsAndCustomerByIdIn joins on this column.
- Impact: Full scan of order_items on every join as the table grows.
- Recommendation: New migration: CREATE INDEX idx_order_items_order_id ON order_items (order_id);

[HIGH] Metrics counter registered on every request (silent memory leak)

- Location: src/main/java/org/example/orderservice/metrics/OrderMetrics.java:41–55
- Description: Counter.builder(...).tag("customer_id", ...).register(registry) is called inside recordOrderCreated() and recordStatusChanged() on every invocation. While Micrometer deduplicates by name+tags, tagging by customer_id UUID creates one time-series per customer — unbounded high
  cardinality. With 100k customers this bloats Prometheus storage and crashes Grafana dashboards.
- Impact: Memory leak in the meter registry; Prometheus scrape timeouts; Grafana slowness.
- Recommendation: Remove the customer_id tag from recordOrderCreated entirely (use it only in logs). For recordStatusChanged, the from_status/to_status tags are bounded (finite enum values) and are fine. Pre-register the status-change counters in the constructor.

  ---                                                                                                                                                                                                                                                                                                          
Test Coverage & Quality

[HIGH] Zero unit tests for CustomerServiceImpl

- Location: src/test/java/org/example/orderservice/service/ — only OrderServiceImplTest.java exists.
- Description: CustomerServiceImpl contains the most security-sensitive logic: password hashing, credential verification, ownership checks on getCustomerById, admin-only gate on getCustomerByEmail. None of this is unit-tested.
- Impact: A regression in password hashing or ownership logic won't be caught until integration tests run — or at all, if the specific path isn't covered.
- Recommendation: Create CustomerServiceImplTest covering: createCustomer (success + duplicate email), verifyCredentials (wrong password returns same error as unknown email), getCustomerById ownership check, getCustomerByEmail admin gate.

[MEDIUM] Dead H2 config in test profile misleads developers

- Location: src/test/resources/application-test.yml:3–11
- Description: The file configures H2, but BaseIntegrationTest.@DynamicPropertySource unconditionally overrides the datasource with PostgreSQL container config. The H2 settings are never used.
- Impact: Developers reading the test profile believe tests run against H2; they may write H2-compatible SQL that silently fails on real PostgreSQL.
- Recommendation: Remove the H2 config entirely from application-test.yml. Document in the README that tests require Docker.

[MEDIUM] cancelOrder ownership check has no dedicated authorization test

- Location: src/test/java/org/example/orderservice/controller/AuthorizationIntegrationTest.java
- Description: DELETE /api/v1/orders/{orderNumber} tests exist, but they verify the cancel endpoint returns 204/403 without separately asserting that the ownership check inside cancelOrder itself (not just the HTTP layer) is exercised.
- Impact: If the ownership check is accidentally removed from cancelOrder, existing tests may not catch it.
- Recommendation: Add should_return403_when_nonOwnerCancelsBypassingProxy — create an order as user A, attempt cancel as user B, assert 403.

  ---                                                                                                                                                                                                                                                                                                          
Code Style & Conventions

[LOW] main method missing public modifier

- Location: src/main/java/org/example/orderservice/OrderServiceApplication.java:9
- Description: static void main(String[] args) is package-private. The JVM will still invoke it, but it violates the standard Java entry-point convention.
- Recommendation: public static void main(String[] args)

  ---                                                                                                                                                                                                                                                                                                          
Configuration & Dependencies

[HIGH] No application-prod.yml — actuator exposure uncontrolled in production

- Location: src/main/resources/ — no application-prod.yml exists.
- Description: The default application.yml exposes health, info, metrics, prometheus actuator endpoints. There is no production profile that restricts this. If the service is accidentally deployed without a profile override, internal metrics are publicly accessible.
- Recommendation: Create application-prod.yml that restricts to health, prometheus only, and sets logging.level.root: WARN.

[MEDIUM] application-test.yml H2 config is dead (see Tests section)

[MEDIUM] Correlation ID not propagated into events

- Location: src/main/java/org/example/orderservice/event/OrderCreatedEvent.java, OrderStatusChangedEvent.java
- Description: Neither event record carries a correlationId. OrderEventListener runs in a separate thread after commit — the MDC correlation ID from the original request is lost.
- Impact: Log lines from event listeners cannot be correlated with the originating HTTP request.
- Recommendation: Add String correlationId to both event records, populate from MDC.get("correlationId") at publish time, and restore in the listener: MDC.put("correlationId", event.correlationId()).

  ---                                                                                                                                                                                                                                                                                                          
Quick Wins

1. Externalize JWT secret — Replace application.yml:67 value with ${JWT_SECRET}. 1-line change, closes the only Critical issue.
2. Add two missing DB indexes — One migration file, two CREATE INDEX statements. Eliminates full table scans on status and order_items.order_id.
3. Remove customer_id tag from metrics counter — Delete line 44 in OrderMetrics.java. Stops the memory leak immediately.
4. Remove dead H2 config from application-test.yml — Delete the file or clear it. Prevents developer confusion.
5. Add public to main and create application-prod.yml — Both are 5-minute fixes that close a style issue and a production exposure gap.

Strategic Recommendations

1. Write CustomerServiceImplTest — This is the most security-sensitive class with zero unit coverage. A half-day investment protects credential verification, password hashing, and ownership logic from silent regressions.
2. Add Caffeine cache to RateLimitingFilter — Replace ConcurrentHashMap with Caffeine.newBuilder().expireAfterAccess(2, MINUTES).maximumSize(50_000).build(). This bounds memory under attack and evicts stale IP entries automatically.
3. Propagate correlation ID through events — Add correlationId to event records and restore MDC in listeners. This completes the observability story end-to-end and is essential once async event handling is introduced. 