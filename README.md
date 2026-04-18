# order-service

Order management microservice built with Java 25 and Spring Boot 4.0.5.

## Prerequisites

| Tool     | Version   |
|----------|-----------|
| Java     | 25+       |
| Maven    | 3.9+      |
| Docker   | 24+       |
| PostgreSQL | 18      |

Docker is required on all profiles — there is no H2 fallback.

## Build

```bash
mvn clean package -DskipTests
```

## Run locally (dev profile)

Start PostgreSQL first (matches the defaults in `application.yml`):

```bash
docker compose up postgres -d
```

Then run the application:

```bash
mvn spring-boot:run
```

Or with the packaged jar:

```bash
java -jar target/order-service-0.0.1-SNAPSHOT.jar
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Default dev datasource (overridable via env vars):

| Variable      | Default     |
|---------------|-------------|
| `DB_HOST`     | `localhost` |
| `DB_PORT`     | `5432`      |
| `DB_NAME`     | `orderdb`   |
| `DB_USER`     | `orderuser` |
| `DB_PASSWORD` | `orderpass` |

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
| `JWT_SECRET`  | JWT signing secret |

## Docker Compose (full stack)

```bash
docker compose up
```

Starts order-service, PostgreSQL, Prometheus, and Grafana.

| Service    | URL                        |
|------------|----------------------------|
| App        | http://localhost:8080      |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090      |
| Grafana    | http://localhost:3000 (admin/admin) |

## Profiles

| Profile   | Database   | Pool size | Log level |
|-----------|------------|-----------|-----------|
| `default` | PostgreSQL | 5         | DEBUG     |
| `staging` | PostgreSQL | 10        | INFO      |
| `prod`    | PostgreSQL | 30        | WARN      |

## Tests

Tests run against a real PostgreSQL instance via Testcontainers. Docker must be running.

```bash
mvn test
```

## Observability

| Endpoint               | Description        |
|------------------------|--------------------|
| `/actuator/health`     | Health check       |
| `/actuator/metrics`    | Micrometer metrics |
| `/actuator/prometheus` | Prometheus scrape  |
| `/swagger-ui.html`     | Swagger UI         |
| `/v3/api-docs`         | OpenAPI JSON       |
