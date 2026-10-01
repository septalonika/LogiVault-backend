# LogiVault Backend

REST API for managing a shop's warehouse inventory: items, variants, prices, stock and sales orders, with overselling prevention and a full stock change history.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Spring Data JPA · Flyway · Spring Security + JWT · MapStruct · Lombok · springdoc (OpenAPI → Apidog)

## Prerequisites
- JDK 21 (`java -version`)
- Docker Desktop (running)

## Setup
```bash
cp .env.example .env            # then edit the secrets
openssl rand -base64 48         # paste the output as JWT_SECRET in .env

docker compose up -d db         # PostgreSQL 16 on localhost:5433
./mvnw spring-boot:run          # dev profile, reads .env automatically
```

| URL | What |
|---|---|
| http://localhost:8080/api/v1 | API base |
| http://localhost:8080/swagger-ui.html | Swagger UI (dev only) |
| http://localhost:8080/v3/api-docs | OpenAPI spec, import into Apidog (dev only) |
| http://localhost:8080/actuator/health | Health check |

## Tests
```bash
./mvnw test      # unit + integration tests (Docker must be running for Testcontainers)
```

## Environment variables
| Name | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5433` / `logivault` | Database location |
| `DB_USERNAME` / `DB_PASSWORD` | `logivault` / – | Database credentials |
| `JWT_SECRET` | – | Base64 HMAC key, at least 32 bytes |
| `LOGIVAULT_ADMIN_EMAIL` / `LOGIVAULT_ADMIN_PASSWORD` | – | First ADMIN account, created on first start |
| `LOGIVAULT_CORS_ALLOWED_ORIGINS` | empty | Comma-separated allowed origins |
