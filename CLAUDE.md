# Order Service — Project Guidelines

## Project Overview
- Order Service manages the lifecycle of customer orders: creation, payment capture, fulfillment, and cancellation. 
- It is part of a larger microservice ecosystem and communicates with other microservices like: Inventory Service, Payment Service, etc. 
- The cross service communication is done via REST calls, events, etc.

## Tech Stack
- Java 25, Spring Boot 4.0.5, Maven
- PostgreSQL 18, Liquibase for database migrations
- MapStruct (componentModel = "spring") for DTO mapping
- Lombok for boilerplate reduction
- Jakarta Validation for request validation
- SpringDoc OpenAPI for API documentation (if applicable)
- Testcontainers + JUnit 5 for integration tests (if applicable)

## Architecture Rules
- Layered architecture: Controller → Service → Repository
- Controllers must never access repositories directly
- All business logic lives in the service layer
- Use constructor injection everywhere, never field injection
- Every public service method must be covered by a unit test

## Database Conventions
- Table names: snake_case, plural (e.g., orders, order_line_items)
- Column names: snake_case
- Entity field mapping via @Column(name = "...") explicitly — don't rely on implicit naming
- Liquibase changelogs: one file per migration
- Never modify an existing changeset — always add a new one

## Persistence Conventions
- @Transactional belongs on the service layer, never on controllers or repositories
- Use @Transactional(readOnly = true) for read-only operations
- Prefer Spring Data JPA derived queries; use @Query with JPQL for complex cases
- Avoid native SQL unless there's a clear performance justification
- Audit fields (createdAt, updatedAt) via @EntityListeners(AuditingEntityListener.class)

## API Conventions
- POST returns 201 + Location header, PUT/PATCH return 200, DELETE returns 204
- Paginated list endpoints use Spring's Pageable with default size 20, max 100
- Request bodies are validated with @Valid; validation errors return 400 with a structured error response: { "errors": [{ "field": "...", "message": "..." }] }
- Use @PathVariable for resource identity, @RequestParam for filtering/sorting

## Events / Integration
- Prefer REST for communicating with other services
- Outbound events are published from the service layer after transaction commit
- Event classes live in a shared event package

## Coding Conventions
- Use UUID for all entity primary keys
- Use BigDecimal for monetary values, never double or float
- All REST endpoints must be versioned under /api/v1/
- DTOs are records where possible, otherwise use Lombok @Builder
- Return ResponseEntity from controllers, not raw objects
- Use AssertJ assertions in tests, not JUnit assertEquals

## Error Handling
- Throw custom exceptions (EntityNotFoundException, etc.)
- Never return null from service methods — use Optional or throw
- All exceptions are handled centrally via @RestControllerAdvice

## Logging
- Use SLF4J with `private static final Logger log = LoggerFactory.getLogger(ClassName.class) or Lombok's @Slf4j
- Log at INFO for business events (order created, payment received)
- Log at WARN for recoverable issues, ERROR for unrecoverable
- Never log sensitive data (PII, payment details)

## Testing Conventions
- Unit tests: Mockito for service layer, no Spring context loaded
- Integration tests: @SpringBootTest + Testcontainers for repository and end-to-end API tests
- Test class naming: {ClassName}Test for unit, {ClassName}IntegrationTest for integration
- Test method naming: should_expectedBehavior_when_condition()
- Use @Nested classes to group tests by method or scenario
- Builders or factory methods for test data — no raw constructors in tests

## Things to Avoid
- No System.out.println — use SLF4J logger
- No wildcard imports
- No magic strings — use constants or enums
- No business logic in controllers or entities (except state machine on Order)