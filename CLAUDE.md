# Order Service — Project Guidelines

## Tech Stack
- Java 25, Spring Boot 4.0.5, Maven
- PostgreSQL 18, Liquibase for database migrations
- MapStruct for DTO mapping, Lombok for boilerplate reduction

## Architecture Rules
- Layered architecture: Controller → Service → Repository
- Controllers must never access repositories directly
- All business logic lives in the service layer
- Use constructor injection everywhere, never field injection
- Every public service method must be covered by a unit test

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

## Things to Avoid
- No System.out.println — use SLF4J logger
- No wildcard imports
- No magic strings — use constants or enums
- No business logic in controllers or entities (except state machine on Order)