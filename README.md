# Product Audit Service

Spring Boot application for managing users, allergies, and products with full audit history.
Supports two data-access modes that can be switched via configuration: **direct JPA** access to PostgreSQL or **delegated REST** access to a remote data service.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Data-Access Modes](#data-access-modes)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [REST API Reference](#rest-api-reference)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)

---

## Architecture Overview

The application follows a **Port & Adapter** (Hexagonal) pattern for the data layer:

```
Controllers / Services
        │
        ┿
  [ Port interfaces ]          ℿ application/port/
        │
   ┌────┴────┐
   │         │
   ┿         ┿
JPA Adapters  REST Adapters    ℿ infrastructure/persistence/{jpa,rest}/
(PostgreSQL)  (Remote service)
```

Services and controllers depend only on the port interfaces (`UserDataPort`, `ProductDataPort`, `AllergyDataPort`, `ProductAuditPort`). The concrete implementation — JPA or REST — is selected at startup via `@ConditionalOnProperty`.

---

## Data-Access Modes

### `mode: jpa` (default)

The application connects directly to a PostgreSQL database.

- Spring Data JPA repositories handle all queries.
- Hibernate Envers records a full audit trail for products.
- Liquibase manages schema migrations.
- Docker Compose starts PostgreSQL automatically.

### `mode: rest`

The application delegates every data operation to a separate **data service** over HTTP using `RestTemplate`.

- No direct database queries are made by this application.
- The remote data service is responsible for persistence and audit history.
- Liquibase, Docker Compose, and the local DataSource can be disabled when this mode is used in production (see [Configuration](#configuration)).

**Remote API contract** expected by the REST adapters:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/users` | List all users (with allergies) |
| `GET` | `/api/users?nameContains={name}` | Filter users by partial name |
| `POST` | `/api/users` | Create a user `{"name":"..."}` |
| `PUT` | `/api/users/{id}` | Update user name `{"name":"..."}` |
| `GET` | `/api/users/count` | Total user count |
| `GET` | `/api/allergies` | List all allergies |
| `POST` | `/api/allergies` | Create an allergy `{"name":"...","severity":"..."}` |
| `GET` | `/api/allergies/count` | Total allergy count |
| `GET` | `/api/products/{id}` | Get product by id |
| `PUT` | `/api/products/{id}` | Update product |
| `GET` | `/api/products/{id}/audit/revisions` | List audit revision numbers |
| `GET` | `/api/products/{id}/audit/{revision}` | Get product snapshot at revision |

---

## Configuration

All configuration lives in `src/main/resources/application.yaml`.

### Key properties

```yaml
app:
  datasource:
    mode: jpa          # jpa (default) | rest

  data-service:
    url: http://localhost:8081   # base URL of the remote data service (mode=rest only)
```

### Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://127.0.0.1:5433/demo` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `DATA_SERVICE_URL` | `http://localhost:8081` | Remote data service base URL |

### Running in REST mode

Set the property or pass it as a JVM argument:

```bash
# via command line
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.datasource.mode=rest --app.data-service.url=http://localhost:8081"
```

When using REST mode you may also want to disable the local datasource:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
  docker:
    compose:
      enabled: false
  liquibase:
    enabled: false
```

---

## Running the Application

### Prerequisites

- Java 21+
- Docker (for the embedded Docker Compose PostgreSQL)

### Start (REST mode — default)

The service defaults to `mode: rest`. **Start [data-service](https://github.com/PauLopNun/data-service) first** — it manages PostgreSQL and exposes the API on port 8081.

```bash
# 1. Start data-service (manages PostgreSQL + exposes /api/* endpoints)
cd ../data-service
./mvnw spring-boot:run

# 2. Start this service (delegates all data operations to data-service)
cd ../product-audit-service-practice
./mvnw spring-boot:run
```

### Start (JPA mode — standalone, no data-service needed)

```yaml
# application.yaml — change these two properties:
app:
  datasource:
    mode: jpa
spring:
  docker:
    compose:
      enabled: true
```

```bash
./mvnw spring-boot:run
```

Spring Boot auto-starts the PostgreSQL container defined in `docker-compose.yml` (port `5433`).
Liquibase applies all migrations and loads the 40-product seed from CSV.
On first run a `CommandLineRunner` populates 5 allergies and 30 users.

### Run tests

```bash
./mvnw test
```

Tests use the `test` profile: H2 in-memory database, Docker Compose and Liquibase disabled.

### Swagger UI

Once running, the interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

---

## REST API Reference

### Users

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/users` | Get all users (JOIN FETCH allergies) |
| `GET` | `/users/allergies` | Get all users (separate allergy load) |
| `GET` | `/users/name-like/{name}` | Search users by partial name |
| `POST` | `/users/{name}` | Create a new user |
| `PUT` | `/users/update-name/{id}/{name}` | Rename a user |

### Products & Audit

| Method | Path | Description |
|--------|------|-------------|
| `PUT` | `/products/{id}` | Partial update of a product |
| `GET` | `/products/{id}/audit/revisions` | List all revision numbers for a product |
| `GET` | `/products/{id}/audit/{revision}` | Get product state at a specific revision |
| `POST` | `/products/{id}/audit/revert/{revision}` | Revert product to a previous revision |
| `GET` | `/products/{id}/audit/diff?from=X&to=Y` | Field-level diff between two revisions |

#### Example: update a product

```bash
curl -X PUT http://localhost:8080/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "iPhone 15", "price": 1199.99}'
```

#### Example: view audit diff

```bash
curl "http://localhost:8080/products/1/audit/diff?from=1&to=2"
```

Response:
```json
{
  "name": {"from": "iPhone 14", "to": "iPhone 15"},
  "price": {"from": 999.99, "to": 1199.99}
}
```

---

## Project Structure

```
src/main/java/com/example/demo/
├── DemoApplication.java                    # Application entry point
│
├── application/
│   ├── port/                               # Port interfaces (data-access contracts)
│   │   ├── UserDataPort.java
│   │   ├── ProductDataPort.java
│   │   ├── AllergyDataPort.java
│   │   └── ProductAuditPort.java
│   └── service/                            # Business logic (mode-agnostic)
│       ├── UserService.java
│       └── ProductAuditService.java
│
├── controller/                             # REST controllers
│   ├── UserController.java
│   └── ProductController.java
│
├── domain/                                 # JPA entities / domain model
│   ├── User.java
│   ├── Product.java
│   ├── Allergy.java
│   └── RevisionInfo.java
│
├── model/                                  # API DTOs
│   ├── UserDTO.java
│   └── AllergyDTO.java
│
├── repository/                             # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   └── AllergyRepository.java
│
└── infrastructure/
    ├── config/
    │   └── RestClientConfig.java           # RestTemplate bean (mode=rest only)
    ├── persistence/
    │   ├── jpa/                            # JPA adapters (mode=jpa, default)
    │   │   ├── UserJpaAdapter.java
    │   │   ├── ProductJpaAdapter.java
    │   │   ├── AllergyJpaAdapter.java
    │   │   └── ProductAuditJpaAdapter.java
    │   └── rest/                           # REST adapters (mode=rest)
    │       ├── UserRestAdapter.java
    │       ├── ProductRestAdapter.java
    │       ├── AllergyRestAdapter.java
    │       ├── ProductAuditRestAdapter.java
    │       └── dto/                        # Response DTOs for remote service
    │           ├── UserResponse.java
    │           ├── AllergyResponse.java
    │           └── ProductResponse.java
    └── seeder/
        └── DataSeeder.java                 # Seed data on startup (mode=jpa only)
```

---

## Technology Stack

| Technology | Version | Role |
|------------|---------|------|
| Spring Boot | 4.0.4 | Application framework |
| Spring Data JPA | — | ORM / repository abstraction |
| Hibernate Envers | — | Product audit history |
| Liquibase | — | Database schema migrations |
| PostgreSQL | 16 | Production database |
| H2 | — | In-memory DB for tests |
| RestTemplate | — | HTTP client for REST mode |
| Lombok | — | Boilerplate reduction |
| springdoc-openapi | 2.5.0 | Swagger UI / OpenAPI docs |
| Docker Compose | — | Auto-managed PostgreSQL container |
