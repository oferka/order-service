# order-service

Order management microservice built with Java 25 and Spring Boot 4.0.5.

## Prerequisites

| Tool        | Version             |
|-------------|---------------------|
| Java        | 26+                 |
| Maven       | 3.9+                |
| Docker      | 24+ (optional)      |
| PostgreSQL  | 18 (staging / prod) |

## Build

```bash
mvn clean package -DskipTests
```

## Run locally (H2 in-memory, dev profile)

```bash
mvn spring-boot:run
```

Or with the packaged jar:

```bash
java -jar target/order-service-0.0.1-SNAPSHOT.jar
```

- H2 console: http://localhost:8080/h2-console  
  JDBC URL: `jdbc:h2:mem:orderdb`
- Swagger UI: http://localhost:8080/swagger-ui.html

## Run with a specific profile

```bash
java -jar target/order-service-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=staging
```

### Required environment variables (staging / prod)

| Variable      | Description       |
|---------------|-------------------|
| `DB_HOST`     | PostgreSQL host   |
| `DB_PORT`     | PostgreSQL port   |
| `DB_NAME`     | Database name     |
| `DB_USER`     | Database username |
| `DB_PASSWORD` | Database password |

## Docker

### Build image

```bash
docker build -t order-service:latest .
```

### Run container

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=staging \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  -e DB_NAME=orders \
  -e DB_USER=postgres \
  -e DB_PASSWORD=secret \
  order-service:latest
```

## Profiles

| Profile   | Database   | Connection pool | Log level |
|-----------|------------|-----------------|-----------|
| `default` | H2         | N/A             | DEBUG     |
| `staging` | PostgreSQL | 10              | INFO      |
| `prod`    | PostgreSQL | 30              | WARN      |

## Observability

| Endpoint                          | Description           |
|-----------------------------------|-----------------------|
| `/actuator/health`                | Health check          |
| `/actuator/metrics`               | Micrometer metrics    |
| `/actuator/prometheus`            | Prometheus scrape     |
| `/swagger-ui.html`                | Swagger UI            |
| `/v3/api-docs`                    | OpenAPI JSON          |
