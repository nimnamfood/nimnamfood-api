# nimnamfood-api

Spring Boot backend API for Nimnamfood.

## Prerequisites

- **Java 21**
- **Docker** (for the PostgreSQL database)
- **Firebase service account key**

## Running locally

1. Add the Firebase service account key in `src/main/resources/nimnamfood-storage-service-account-key-dev.json`


2. Start the PostgreSQL database:

```bash
docker compose up -d
```

3. Run the application:

```bash
./mvnw spring-boot:run
```

The API starts on [http://localhost:8080](http://localhost:8080).

## Running tests

Tests use Testcontainers (Docker must be running):

```bash
./mvnw test
```
