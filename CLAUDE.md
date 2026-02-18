# CLAUDE.md — nimnamfood-api

## Project Overview

**nimnamfood-api** is a Spring Boot 3.2.2 REST API ("Daily Meal Assistant") for managing food recipes, ingredients, tags, and illustrations. It is built on Java 21, uses PostgreSQL for persistence, and applies CQRS + DDD architectural patterns via an in-house framework in the `vtertre` package.

---

## Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.2 |
| Database | PostgreSQL 14 |
| DB migrations | Flyway |
| ORM/data access | Spring Data JDBC |
| Validation | Hibernate Validator 8 |
| Storage | Google Firebase / Cloud Storage |
| Testing | JUnit 5, Spring Boot Test, Testcontainers |
| Build | Maven (mvnw wrapper) |
| Utilities | Google Guava |

---

## Build & Development Commands

```bash
# Start local PostgreSQL (Docker required)
docker compose up -d

# Run the application (local profile is active by default)
./mvnw spring-boot:run

# Run all tests (Docker must be running for Testcontainers)
./mvnw test

# Full build + verify (used in CI)
mvn --batch-mode --no-transfer-progress verify
```

The API listens on `http://localhost:8080` by default (override with `PORT` env var).

---

## Project Structure

```
src/
├── main/java/
│   ├── nimnamfood/              # Application code
│   │   ├── NimnamfoodApiApplication.java   # Spring Boot entry point
│   │   ├── NimnamfoodConfiguration.java    # Main Spring config (buses, repos)
│   │   ├── NimnamfoodExceptionHandler.java # Global exception -> HTTP mapping
│   │   ├── JacksonConfiguration.java       # Two ObjectMapper beans (default + jsonb)
│   │   ├── StorageConfiguration.java       # Firebase Storage bean
│   │   │
│   │   ├── adapter/storage/     # Firebase/GCS storage adapter
│   │   ├── command/             # Commands and handlers (write side)
│   │   │   ├── illustration/
│   │   │   ├── ingredient/
│   │   │   ├── recipe/
│   │   │   └── tag/
│   │   ├── infrastructure/repository/
│   │   │   ├── jdbc/            # Spring Data JDBC repository implementations
│   │   │   └── memory/          # In-memory repository implementations (for dev/test)
│   │   ├── model/               # Domain model (aggregate roots, domain events, repos)
│   │   │   ├── ingredient/
│   │   │   ├── recipe/
│   │   │   └── tag/
│   │   ├── query/               # Queries and handlers (read side)
│   │   │   ├── ingredient/
│   │   │   ├── recipe/
│   │   │   └── tag/
│   │   ├── service/             # RecipeService (illustration lifecycle)
│   │   └── web/                 # REST controllers + converters
│   │
│   └── vtertre/                 # In-house DDD/CQRS/bus framework
│       ├── command/             # Command, CommandBus, CommandHandler, CommandMiddleware
│       ├── ddd/                 # AggregateRoot, Entity, Repository, DomainEvent, EventBus
│       ├── infrastructure/bus/  # Async command, event, and query bus implementations
│       ├── infrastructure/persistence/ # Base JDBC/memory DBO and repository classes
│       └── query/               # Query, QueryBus, QueryHandler, QueryHandlerJdbc
│
├── main/resources/
│   ├── application.properties           # Base config (virtual threads, Flyway, storage)
│   ├── application-local.properties     # Local dev config (Docker PostgreSQL)
│   ├── application-production.properties# Production config (env vars)
│   ├── db/migration/postgresql/         # Flyway SQL migrations (V1–V9)
│   └── logback.xml
│
└── test/java/
    ├── nimnamfood/              # Application-level tests
    └── vtertre/                 # Framework-level tests
```

---

## Architecture: CQRS + DDD

The codebase strictly separates reads from writes using the Command Query Responsibility Segregation (CQRS) pattern, underpinned by Domain-Driven Design (DDD) building blocks from the `vtertre` package.

### Write Side (Commands)

1. A REST controller dispatches a `Command` object onto the `CommandBus`.
2. The `CommandBus` (async, uses a fixed-thread-pool executor) runs the command through a middleware chain (validation → handler invocation).
3. The `CommandHandler` executes domain logic, persists the aggregate root via `Repositories`, and returns a `Tuple<TResponse, List<DomainEvent>>`.
4. The `EventPublisherMiddleware` publishes the returned domain events onto the `EventBus`.

```
Controller → CommandBus → CommandValidator → InvokeCommandHandlerMiddleware
                                                      ↓
                                             CommandHandler.execute()
                                                      ↓ returns Tuple<Result, Events>
                                             EventPublisherMiddleware.publish(events)
                                                      ↓
                                                   EventBus
```

### Read Side (Queries)

1. A REST controller dispatches a `Query` object onto the `QueryBus`.
2. The `QueryBus` (async, uses a virtual-thread-per-task executor) routes to the matching `QueryHandler`.
3. Query handlers that extend `QueryHandlerJdbc` have direct access to `NamedParameterJdbcTemplate` and query denormalized **view tables** in PostgreSQL.

### Event Projections

`EventCaptor<TEvent>` implementations listen to domain events and update the denormalized view tables synchronously (or asynchronously if not annotated with `@Synced`). This keeps the read-side views consistent after writes.

Example flow for creating a recipe:
```
CreateRecipeCommand → CreateRecipeCommandHandler → Recipe.factory().create()
  → Repositories.recipes().add(recipe)
  → RecipeCreated event published
    → OnRecipeCreatedFillSummary   (updates view_recipes)
    → OnRecipeCreatedFillSearchSummary (updates view_recipe_search)
```

---

## Domain Model

Three aggregate roots, each with a corresponding domain event for creation and update:

| Aggregate | Created event | Changed event | Repository interface |
|---|---|---|---|
| `Recipe` | `RecipeCreated` | `RecipeChanged` | `RecipeRepository` |
| `Ingredient` | `IngredientCreated` | `IngredientChanged` | `IngredientRepository` |
| `Tag` | `TagCreated` | — | `TagRepository` |

`Repositories` is a static-access singleton initialized at startup. Call `Repositories.recipes()`, `Repositories.ingredients()`, or `Repositories.tags()` from command handlers.

---

## REST API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Health check |
| `GET` | `/recipes` | Search recipes (`q`, `tags`, `skip`, `limit`) |
| `POST` | `/recipes` | Create a recipe |
| `GET` | `/recipes/{id}` | Get a single recipe |
| `PUT` | `/recipes/{id}` | Update a recipe |
| `GET` | `/ingredients` | List ingredients |
| `POST` | `/ingredients` | Create an ingredient |
| `PUT` | `/ingredients/{id}` | Update an ingredient |
| `GET` | `/ingredient-units` | List available units |
| `GET` | `/tags` | List tags |
| `POST` | `/tags` | Create a tag |
| `POST` | `/illustrations` | Upload an illustration (multipart) |

### Tag Filter Query Syntax (`?tags=`)

The `tags` parameter on `GET /recipes` uses a mini query language parsed by `StringToTagFilterQueryConverter`:

| Syntax | Meaning |
|---|---|
| `tagName` | Recipe must have this tag |
| `!tagName` | Recipe must NOT have this tag |
| `(tag1\|tag2)` | Recipe must have at least one of these tags |
| Combined (comma-separated) | All filters must match |

---

## Database & Migrations

Flyway migrations live in `src/main/resources/db/migration/postgresql/`. Always add new migrations as `V{N+1}__description.sql`.

### Core Tables (write side)
- `recipes`, `recipe_ingredients`, `recipe_tags`
- `ingredients`
- `tags`

### View Tables (read side — denormalized for queries)
- `view_recipe_search` — lightweight search results (id, name, illustration_url, tags JSON, creation_date_time)
- `view_recipes` — full recipe detail (id, name, illustration JSON, portions_count, instructions, ingredients JSONB, tags JSONB)
- `view_part_recipe_tags` — tag lookup for recipe views
- `view_part_recipe_ingredients` — ingredient lookup for recipe views
- `view_part_default_ingredients` — ingredient list view
- `view_part_default_tags` — tag list view

View tables are kept in sync by `EventCaptor` projections — **never update view tables directly**; emit a domain event instead.

---

## Configuration Profiles

### `local` (default for development)
Set in `application-local.properties`. Connects to the Docker PostgreSQL container:
```
spring.datasource.url=jdbc:postgresql:postgres
spring.datasource.username=postgres
spring.datasource.password=password
```
Toggle in-memory repositories (no database needed) with:
```
nimnamfood.data.inmemory=true
spring.flyway.enabled=false
```

### `production`
Set in `application-production.properties`. All values are read from environment variables:
`DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `STORAGE_BUCKET`, `STORAGE_PROJECT_ID`, `CORS_ALLOWED_ORIGINS`.

---

## Storage (Firebase / Google Cloud Storage)

Illustrations follow a two-step lifecycle managed by `RecipeService`:

1. **Upload** (`POST /illustrations`): File stored in `pending/{uuid}.webp`.
2. **Activate** (on recipe create/update with `illustrationId`): Copied from `pending/` to `live/recipes/`, then the `pending/` blob is deleted.
3. **Delete** (on recipe update replacing illustration): Old `live/recipes/` blob is deleted.

The service account key JSON file must be placed in the classpath:
- Dev: `nimnamfood-storage-service-account-key-dev.json`
- Prod: `storage-service-account-key-prod.json`

---

## Testing

### Test Categories

**Unit tests** (no Docker needed): Use `MemoryRepositories` via the `WithMemoryRepositories` JUnit extension. These test command handlers, query projections, and domain logic in isolation.

**Integration tests** (Docker required, tagged `@Tag("IO")`): Extend `PostgresTestContainerBase`, which starts a `postgres:14` Testcontainer. Use `WithJdbcRepositories` JUnit extension to wire JDBC repos.

### Speeding Up Integration Tests

Enable container reuse by adding to `~/.testcontainers.properties`:
```
testcontainers.reuse.enable=true
```
You must manually clean up containers after use.

### Running Tests

```bash
# All tests (Docker must be running)
./mvnw test

# Skip IO-tagged tests if Docker is unavailable
./mvnw test -Dgroups='!IO'
```

---

## Key Conventions

### Adding a New Command

1. Create `XxxCommand implements Command<TResponse>` in `nimnamfood/command/<domain>/`.
2. Add validation annotations (`@NotBlank`, `@NotNull`, `@UUID`, etc.) directly on public fields.
3. Create `XxxCommandHandler implements CommandHandler<XxxCommand, TResponse>` annotated `@Component`.
4. The handler returns `Tuple.of(result, List.of(domainEvent))`. If no events are needed, return `Tuple.of(result, Collections.emptyList())`.

### Adding a New Query

1. Create `XxxQuery implements Query<TResponse>` (or extend `QueryHandlerJdbc` base class if JDBC).
2. Create `XxxHandler` annotated `@Component` implementing `QueryHandler<XxxQuery, TResponse>` (or `QueryHandlerJdbc`).
3. Use `NamedParameterJdbcTemplate` to query view tables — **read from view tables only**.

### Adding a New Event Captor (Projection)

1. Create a class annotated `@Component` implementing `EventCaptor<TEvent>`.
2. Add `@Synced` if the projection must run in the same transaction as the command.
3. The captor will be auto-discovered and registered with `EventBusAsync` at startup.

### Adding a New Domain Entity

1. Extend `BaseAggregateRootWithUuid` for aggregate roots with UUID identity.
2. Use the `Factory` inner class pattern with `create()` returning `Tuple<Entity, DomainEvent>`.
3. Mutation methods (`updated(...)`) also return a `Tuple<Entity, DomainEvent>`.

### Flyway Migrations

- File naming: `V{N}__snake_case_description.sql` in `src/main/resources/db/migration/postgresql/`.
- Never modify an existing migration — always add a new version.
- The placeholder `${storage-bucket}` is substituted by Flyway from `spring.flyway.placeholders.storage-bucket`.

### Jackson / JSON

Two `ObjectMapper` beans:
- **Default** (`@Primary`): Used by Spring MVC for request/response bodies (camelCase).
- **`@Qualifier("Jsonb")`**: Used for reading/writing JSONB columns from PostgreSQL (snake_case via `SnakeCaseStrategy`). Inject with `@Qualifier("Jsonb")`.

### Exception Handling

`NimnamfoodExceptionHandler` maps:
- `ValidationException` → `400 Bad Request` with `{"errors": [...]}`
- `MissingAggregateRootException` → `404 Not Found` with `{"error": "..."}`
- `DuplicateKeyException` → `400 Bad Request` with `{"error": "..."}`

### CORS

Configured via `cors.allowed-origins` property (comma-separated origins). Defaults to `*` in dev.

---

## CI

GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push:
- Sets up JDK 21 (Temurin distribution) with Maven cache.
- Runs `mvn --batch-mode --no-transfer-progress verify` which compiles and runs all tests (including Testcontainers-based ones using the Docker service provided by GitHub-hosted runners).
