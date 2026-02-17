# nimnamfood-api

Spring Boot backend API for Nimnamfood.

## Prerequisites

- **Java 21**
- **Docker** (for the PostgreSQL database)

## Running locally

1. Start the PostgreSQL database:

```bash
docker compose up -d
```

2. Run the application:

```bash
./mvnw spring-boot:run
```

The API starts on [http://localhost:8080](http://localhost:8080).

## Running tests

Tests use Testcontainers (Docker must be running):

```bash
./mvnw test
```
