# Multi-user implementation plan — nimnamfood-api

This document is a self-contained implementation plan to turn nimnamfood-api into a
multi-user application with Clerk authentication and household-based data ownership.
It is written to be executed phase by phase by an implementing agent with no prior
context. Read the whole document before starting. **Do each phase in order; run
`./mvnw test` after each phase (Docker must be running — tests use Testcontainers).**

Commit convention: French, imperative, lowercase, no prefix (e.g.
`ajouter les tables users et households`). One commit per phase minimum.

> **Note for Vincent (before handing this off):** re-read **Phase 4f** (copy-on-use
> for tags/ingredients) and the **Phase 7 endpoint table** (households, invites,
> join). Those two sections encode product decisions, not just technical ones —
> exact copy-on-use semantics, invite links being multi-use with 7-day expiry, any
> member being allowed to invite. They deserve your sign-off rather than the
> implementing model's; everything else in this plan is mechanical.

---

## 1. Decisions already made (do not re-litigate)

- **Clerk is used for authentication only.** The API is a stateless OAuth2 resource
  server validating Clerk-issued JWTs. Only the `sub` claim (Clerk user id, format
  `user_xxx`) is used. No Clerk SDK in the backend, no webhooks.
- **All domain data belongs to exactly one household** (`household_id` column). There
  is no user-owned data. Every user gets a **personal household** auto-created on
  first authenticated request (lazy provisioning). Users can belong to several
  households (create one, or join via invite token).
- **Reads are merged across all the user's households** (list endpoints return rows
  from every household the user belongs to). By-id reads return 404 if the row's
  household is not among the user's memberships (no existence leak).
- **Writes target one household**, chosen by the client via an optional `householdId`
  field on commands; when absent, the user's personal household is used. Targeting a
  household the user is not a member of is a business error.
- **Tags and ingredients are per-household, with copy-on-use**: when a recipe
  create/update references a tag/ingredient from *another* of the user's households,
  the handler transparently find-or-creates an equivalent (same name) in the target
  household and substitutes the id. References to tags/ingredients of households the
  user does not belong to are rejected.
- **Households are NOT Clerk Organizations.** They are plain domain tables here.

## 2. Architecture facts you must know before touching anything

- **CQRS buses are async.** `CommandBusAsync` runs the handler on a `Computation`
  executor; `QueryBusAsync` runs the *entire middleware chain + handler* on the `Io`
  executor (`NimnamfoodConfiguration.java`). Therefore **request-thread ThreadLocals
  (including `SecurityContextHolder`) do NOT reach handlers.** The user context must
  be carried explicitly on command/query objects, set by the web layer before
  dispatch. This plan does exactly that.
- **Projections are event-driven.** Command handlers return
  `Tuple<TResponse, List<DomainEvent>>`; an `EventPublisherMiddleware` publishes the
  events, and `EventCaptor<E>` components (in `nimnamfood/query/*/projection/`)
  insert/update the `view_*` tables via `JdbcClient`. So **`*Created` domain events
  must carry `householdId`** for the captors to write it into view rows. `*Changed`
  events do not need it (updates are keyed by id, and the household of a row never
  changes).
- **Commands are mutable classes with public fields, deserialized directly from the
  request body** by Spring MVC (e.g. `RecipesResource#create`). Any context field we
  add must be `@JsonIgnore` and always overwritten by the resource layer.
- **Queries extend the abstract `vtertre.query.Query<T>`** (has `limit`/`skip` with
  fluent setters). `vtertre.*` is a generic library layer: **do not put nimnamfood
  domain types (like UserContext) inside `vtertre`**. Introduce a nimnamfood-level
  base class instead (Phase 3).
- **Handlers access repositories via the static `nimnamfood.model.Repositories`**
  (`Repositories.recipes()` etc.). There are JDBC implementations
  (`infrastructure/repository/jdbc/`) and in-memory ones
  (`infrastructure/repository/memory/`), selected by `nimnamfood.data.inmemory`.
  Both must be kept compiling. (Note: memory mode is already half-broken since query
  handlers always read SQL views; do not invest in it beyond compiling + mirroring
  filter behavior.)
- **Tests do not run Flyway.** `src/test/resources/application.properties` sets
  `spring.flyway.enabled=false` and `spring.sql.init.mode=always`; the schema comes
  from **`src/test/resources/schema.sql`**, which must be updated in lockstep with
  every migration. Handler tests extend
  `vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase`
  (`@DataJdbcTest` + Testcontainers postgres:14, reused container).
- Flyway migrations live in `src/main/resources/db/migration/postgresql/` (next
  number: **V14**). Flyway placeholders are already used
  (`spring.flyway.placeholders.storage-bucket`).
- Existing view tables that will need `household_id`: `view_tags`,
  `view_ingredients`, `view_part_recipe_tags`, `view_part_recipe_ingredients`,
  `view_recipe_search`, `view_recipes`, `view_plan_search`, `view_plans`.
- Deployment reads config from env vars (`application-production.properties`).
  Health check is `GET /` (`HealthResource`) — it must remain unauthenticated.

---

## Phase 0 — Clerk setup (manual, for Vincent — the agent documents, does not do)

1. Create a Clerk application (dashboard.clerk.com). Enable the sign-in methods you
   want (email + Google recommended).
2. Note the **issuer URI** of the dev instance: `https://<slug>.clerk.accounts.dev`
   (visible in Clerk dashboard → API Keys → Show JWT public key → issuer, or in the
   Frontend API URL). The JWKS/OIDC discovery endpoints hang off this URI, which is
   all Spring needs.
3. Sign in once (e.g. via Clerk's hosted account portal) and copy your **Clerk user
   id** (`user_xxx`, dashboard → Users). It is needed as the Flyway placeholder for
   the data backfill (Phase 2) before running the migration in each environment.

The implementing agent should use the placeholder values wired in Phases 1–2 and
flag clearly at the end which values Vincent must fill in.

## Phase 1 — Spring Security resource server skeleton

**Goal:** every endpoint except `GET /` requires a valid Clerk JWT. No behavior
change otherwise.

1. `pom.xml`: add
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
   </dependency>
   <!-- TESTING -->
   <dependency>
       <groupId>org.springframework.security</groupId>
       <artifactId>spring-security-test</artifactId>
       <scope>test</scope>
   </dependency>
   ```
2. New file `src/main/java/nimnamfood/web/configuration/SecurityConfiguration.java`:
   ```java
   package nimnamfood.web.configuration;

   import org.springframework.context.annotation.Bean;
   import org.springframework.context.annotation.Configuration;
   import org.springframework.security.config.Customizer;
   import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
   import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
   import org.springframework.security.config.http.SessionCreationPolicy;
   import org.springframework.security.web.SecurityFilterChain;

   @Configuration
   @EnableWebSecurity
   public class SecurityConfiguration {
       @Bean
       public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
           http
                   .csrf(AbstractHttpConfigurer::disable)
                   .cors(Customizer.withDefaults())
                   .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                   .authorizeHttpRequests(requests -> requests
                           .requestMatchers("/").permitAll()
                           .anyRequest().authenticated())
                   .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()));
           return http.build();
       }
   }
   ```
   CORS note: `.cors(Customizer.withDefaults())` with no `CorsConfigurationSource`
   bean falls back to the MVC-registered CORS config (the existing
   `CorsConfiguration` WebMvcConfigurer), so preflights keep working. If manual
   testing shows preflight failures, replace the MVC registration with a
   `CorsConfigurationSource` bean built from the same `cors.allowed-origins`
   property.
3. `src/main/resources/application.properties`: add
   ```properties
   spring.security.oauth2.resourceserver.jwt.issuer-uri=${CLERK_ISSUER_URI:https://REPLACE-ME.clerk.accounts.dev}
   ```
   `application-production.properties`: add
   ```properties
   spring.security.oauth2.resourceserver.jwt.issuer-uri=${CLERK_ISSUER_URI}
   ```
4. Existing tests are `@DataJdbcTest` slices or plain unit tests — Spring Security
   does not activate there; they must still pass unchanged (`./mvnw test`).
5. New test `src/test/java/nimnamfood/web/SecurityConfigurationTest.java` using
   `@WebMvcTest(HealthResource.class)` + `@Import(SecurityConfiguration.class)`:
   - `GET /` without token → 200.
   - `GET /recipes` without token → 401 (use `@WebMvcTest({HealthResource.class,
     RecipesResource.class})` and `@MockitoBean CommandBus` / `@MockitoBean QueryBus`;
     a `JwtDecoder` `@MockitoBean` is required so the context starts without
     contacting the issuer).
   - `GET /recipes` with `.with(SecurityMockMvcRequestPostProcessors.jwt())` → 200.

**Acceptance:** `./mvnw test` green; started locally, `curl localhost:8080/` → 200,
`curl localhost:8080/recipes` → 401.

## Phase 2 — Database: users, households, ownership columns

**Goal:** schema knows about users/households; all existing rows backfilled into one
initial household. No Java changes yet (Spring Data JDBC ignores unknown columns; the
new columns get defaults so inserts from not-yet-migrated code still work — see the
temporary `default` below, removed in Phase 8).

1. New migration `V14__add_users_and_households.sql`:
   ```sql
   create table households
   (
       id         uuid primary key,
       name       text        not null,
       type       text        not null default 'SHARED', -- 'PERSONAL' | 'SHARED'
       created_at timestamptz not null default now()
   );

   create table users
   (
       id                    uuid primary key,
       clerk_id              text        not null unique,
       personal_household_id uuid        not null references households (id),
       created_at            timestamptz not null default now()
   );

   create table household_members
   (
       household_id uuid        not null references households (id) on delete cascade,
       user_id      uuid        not null references users (id) on delete cascade,
       role         text        not null default 'MEMBER', -- 'OWNER' | 'MEMBER'
       joined_at    timestamptz not null default now(),
       primary key (household_id, user_id)
   );
   create index idx_household_members_user on household_members (user_id);

   create table household_invites
   (
       token        uuid primary key,
       household_id uuid        not null references households (id) on delete cascade,
       created_by   uuid        not null references users (id),
       created_at   timestamptz not null default now(),
       expires_at   timestamptz not null
   );

   -- Backfill: initial household owned by the pre-existing single user.
   insert into households (id, name, type)
   values ('00000000-0000-4000-8000-000000000001', 'Personnel', 'PERSONAL');

   insert into users (id, clerk_id, personal_household_id)
   values ('00000000-0000-4000-8000-000000000002', '${initial-clerk-user-id}',
           '00000000-0000-4000-8000-000000000001');

   insert into household_members (household_id, user_id, role)
   values ('00000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'OWNER');
   ```
2. New migration `V15__add_household_id_to_entities_and_views.sql`. For **each** of
   `recipes`, `ingredients`, `tags`, `plans`, `view_tags`, `view_ingredients`,
   `view_part_recipe_tags`, `view_part_recipe_ingredients`, `view_recipe_search`,
   `view_recipes`, `view_plan_search`, `view_plans`:
   ```sql
   alter table recipes add column household_id uuid not null
       default '00000000-0000-4000-8000-000000000001';
   create index idx_recipes_household on recipes (household_id);
   ```
   (repeat, adjusting names; the `default` both backfills existing rows and keeps
   pre-Phase-4 code inserting successfully — it is dropped in Phase 8). Add the FK
   only on the four entity tables, not the view tables:
   ```sql
   alter table recipes add constraint fk_recipes_household
       foreign key (household_id) references households (id);
   ```
   Then replace the global unique names:
   ```sql
   alter table tags drop constraint tags_name_key;
   alter table tags add constraint tags_household_name_key unique (household_id, name);
   alter table ingredients drop constraint ingredients_name_key;
   alter table ingredients add constraint ingredients_household_name_key unique (household_id, name);
   ```
   (Those are Postgres default constraint names for `unique(name)` in `V1`. Verify
   against a locally migrated DB with `\d tags` if the drop fails.)
3. Flyway placeholder wiring:
   - `application.properties`:
     `spring.flyway.placeholders.initial-clerk-user-id=${INITIAL_CLERK_USER_ID:user_REPLACE_ME}`
   - `application-production.properties`:
     `spring.flyway.placeholders.initial-clerk-user-id=${INITIAL_CLERK_USER_ID}`
4. **Update `src/test/resources/schema.sql` in lockstep**: add the four new tables,
   add `household_id uuid not null` (NO default — tests must be explicit) to the
   entity and view tables listed above, mirror the new unique constraints, and
   append fixture rows used by all tests:
   ```sql
   insert into households (id, name, type) values
       ('00000000-0000-4000-8000-00000000000a', 'Household A', 'PERSONAL'),
       ('00000000-0000-4000-8000-00000000000b', 'Household B', 'PERSONAL');
   insert into users (id, clerk_id, personal_household_id) values
       ('00000000-0000-4000-8000-0000000000aa', 'user_test_a', '00000000-0000-4000-8000-00000000000a'),
       ('00000000-0000-4000-8000-0000000000bb', 'user_test_b', '00000000-0000-4000-8000-00000000000b');
   insert into household_members (household_id, user_id, role) values
       ('00000000-0000-4000-8000-00000000000a', '00000000-0000-4000-8000-0000000000aa', 'OWNER'),
       ('00000000-0000-4000-8000-00000000000b', '00000000-0000-4000-8000-0000000000bb', 'OWNER');
   ```
   Note: `schema.sql` uses `drop table if exists` before each `create table` — do the
   same for the new tables, and order drops so FK dependents drop first (drop
   `household_invites`, `household_members`, `users`, then `households`).
5. New test constants class `src/test/java/nimnamfood/TestHouseholds.java` exposing
   `public static final UUID HOUSEHOLD_A = UUID.fromString("00000000-0000-4000-8000-00000000000a");`
   (and B, USER_A, USER_B, plus `CLERK_ID_A = "user_test_a"`).

**Acceptance:** `./mvnw test` green (schema.sql compiles, defaults absent in test
schema will surface any helper that forgot household_id — fix helpers in Phase 6 if
any break here; if too many break, temporarily add the default in schema.sql and
remove it in Phase 6). Local run: `docker compose up -d`, start the app, check
Flyway applies V14–V15.

## Phase 3 — UserContext plumbing (web → bus → handlers)

**Goal:** every command/query carries an authenticated `UserContext`; forgetting to
set it is a hard error.

1. New record `src/main/java/nimnamfood/model/user/UserContext.java`:
   ```java
   package nimnamfood.model.user;

   import java.util.Set;
   import java.util.UUID;

   public record UserContext(UUID userId, UUID personalHouseholdId, Set<UUID> householdIds) {
       public boolean isMemberOf(UUID householdId) {
           return this.householdIds.contains(householdId);
       }
   }
   ```
2. New interface `src/main/java/nimnamfood/model/user/HouseholdScoped.java`:
   ```java
   public interface HouseholdScoped {
       UserContext userContext();
       void userContext(UserContext userContext);
   }
   ```
3. **Queries:** new abstract class
   `src/main/java/nimnamfood/query/HouseholdScopedQuery.java`:
   ```java
   public abstract class HouseholdScopedQuery<TResponse> extends Query<TResponse> implements HouseholdScoped {
       private UserContext userContext;

       @Override public UserContext userContext() { return this.userContext; }
       @Override public void userContext(UserContext userContext) { this.userContext = userContext; }
   }
   ```
   Change every nimnamfood query (`FindRecipes`, `GetRecipe`, `FindTags`,
   `FindIngredients`, `GetAllIngredientUnits`, `GetPlans`, `GetPlan`) to extend
   `HouseholdScopedQuery` instead of `Query`.
4. **Commands:** every command class (`CreateRecipeCommand`, `UpdateRecipeCommand`,
   `CreateTagCommand`, `CreateIngredientCommand`, `UpdateIngredientCommand`,
   `GeneratePlanCommand`, `ImportIllustrationCommand`) implements `HouseholdScoped`
   with an explicit field:
   ```java
   @JsonIgnore
   private UserContext userContext;

   @Override public UserContext userContext() { return this.userContext; }
   @Override public void userContext(UserContext userContext) { this.userContext = userContext; }
   ```
   `@JsonIgnore` is mandatory: commands are bound from the request body and clients
   must not be able to inject a context.
5. **User provisioning service.** New
   `src/main/java/nimnamfood/service/UserContextService.java`, a `@Component` using
   `JdbcClient`, with `@Transactional public UserContext resolve(String clerkId)`:
   - `SELECT u.id, u.personal_household_id, m.household_id FROM users u JOIN
     household_members m ON m.user_id = u.id WHERE u.clerk_id = :clerkId` → if rows,
     build the record.
   - If empty: insert `households` row (`gen random UUID`, name `'Personnel'`, type
     `'PERSONAL'`), insert `users` row, insert `household_members` row with role
     `'OWNER'`; return the context. Wrap in `try/catch (DuplicateKeyException)` and
     on catch re-run the select (two concurrent first requests race on
     `users.clerk_id` unique; the transaction rollback discards the loser's orphan
     household).
6. **Argument resolver.** New
   `src/main/java/nimnamfood/web/configuration/UserContextArgumentResolver.java`
   implementing `HandlerMethodArgumentResolver`:
   - `supportsParameter`: parameter type is `UserContext`.
   - `resolveArgument`: get `SecurityContextHolder.getContext().getAuthentication()`,
     cast to `JwtAuthenticationToken`, take `getToken().getSubject()`, return
     `userContextService.resolve(subject)`. (This runs on the request thread, where
     the SecurityContext exists.)
   - Register it: add a `WebMvcConfigurer` override of `addArgumentResolvers` (either
     in the existing `CorsConfiguration`'s anonymous `WebMvcConfigurer` or a new
     `@Configuration` — new class `WebConfiguration` preferred).
7. **Safety-net middlewares** (reject dispatch when context is missing —
   programming-error guard, not user error):
   - `src/main/java/nimnamfood/command/UserContextRequiredMiddleware.java`,
     `@Component @Order(0)` implementing `CommandMiddleware`: if
     `command instanceof HouseholdScoped scoped && scoped.userContext() == null`,
     throw `new IllegalStateException("Command dispatched without user context: " +
     command.getClass().getSimpleName())`; else `nextMiddleware.get()`.
   - Same for queries: `nimnamfood/query/UserContextRequiredMiddleware.java`
     implementing `QueryMiddleware` (`@Component @Order(0)`).
8. **Resources.** Every controller method gains a `UserContext userContext` parameter
   (resolved by the new resolver) and sets it on the command/query before dispatch,
   e.g. in `RecipesResource`:
   ```java
   @PostMapping("/recipes")
   public Future<ResponseEntity<Map<String, UUID>>> create(UserContext userContext,
                                                           @RequestBody CreateRecipeCommand command) {
       command.userContext(userContext);
       ...
   }
   ```
   Apply to all methods of: `RecipesResource`, `RecipeResource`, `TagsResource`,
   `IngredientsResource`, `IngredientResource`, `IngredientUnitsResource`,
   `PlansResource`, `PlanResource`, `IllustrationsResource`. NOT `HealthResource`.

**Acceptance:** compiles; existing handler tests still pass (they call
`handler.execute(...)` directly, bypassing middlewares — they'll set contexts in
Phase 6). `./mvnw test` green.

## Phase 4 — Domain: household ownership in aggregates, events, projections, writes

This is the mechanical heart. Per aggregate, the same recipe (pun intended):

### 4a. Recipe
- `model/recipe/Recipe.java`: add `private final UUID householdId` + getter; both
  constructors take it; `Factory.create(...)` takes `UUID householdId` as **first**
  parameter (update the two convenience overloads too); `recreateFromDbo` reads
  `dbo.getHouseholdId()`. `updated(...)` keeps the existing `householdId` (ownership
  never changes on update).
- `model/recipe/RecipeCreated.java`: add `UUID householdId` component.
  `RecipeChanged`: unchanged.
- `infrastructure/repository/jdbc/recipe/RecipeDbo.java`: add `UUID householdId` +
  getter/setter; `RecipeJdbcRepository.toDbo` maps it.
- `infrastructure/repository/jdbc/recipe/RecipeJdbcRepository.findIdsByTagFilterRequirement`:
  add parameter `UUID householdId`; every SQL branch gains
  `r.household_id = :householdId` (the no-clause branch becomes
  `SELECT r.id FROM recipes r WHERE r.household_id = :householdId`). Update the
  `RecipeRepository` interface and the memory implementation accordingly.
- `command/recipe/CreateRecipeCommand.java`: add
  `@org.hibernate.validator.constraints.UUID public String householdId;` (optional).
- `CreateRecipeCommandHandler`: resolve target household:
  ```java
  final UUID householdId = TargetHousehold.resolve(command);   // see 4e
  ```
  pass to factory. `UpdateRecipeCommandHandler`: after loading, enforce ownership:
  ```java
  if (!command.userContext().isMemberOf(currentRecipe.get().getHouseholdId())) {
      throw new MissingAggregateRootException(command.id);   // 404, no existence leak
  }
  ```
  (`UpdateRecipeCommand` does NOT get a `householdId` field — the recipe stays where
  it is.)
- Projections `query/recipe/projection/`:
  - `OnRecipeCreatedFillSearchSummary` and `OnRecipeCreatedFillSummary`: add
    `household_id` to the INSERT column lists with `:householdId` param from
    `event.householdId()`.
  - `OnRecipeChanged*`: no change (UPDATE by id).
  - `OnTagCreatedFillRecipeViewPart` / `OnIngredientCreatedFillRecipeViewPart` /
    `OnIngredientChangedUpdateRecipeViewPart`: the `view_part_*` tables gained
    `household_id`; created-captors insert it from the event (see 4b/4c).

### 4b. Tag
- `model/tag/Tag.java`: add `householdId` (same pattern as Recipe);
  `TagCreated` gains `UUID householdId`.
- `TagRepository`: add `Optional<Tag> findByHouseholdAndName(UUID householdId, String name)`
  and `List<Tag> getAllById(Set<UUID> ids)` (needed for copy-on-use). Implement in
  JDBC (simple `NamedParameterJdbcTemplate` queries or Spring Data derived methods on
  the CrudRepository, matching how sibling repositories are built) and in memory.
- `TagDbo` + `TagJdbcRepository.toDbo`: map the column.
- `CreateTagCommand`: add optional `@UUID public String householdId;`.
  `CreateTagCommandHandler`: resolve target via `TargetHousehold.resolve(command)`.
- Projection `OnTagCreatedFillSummary` (in `query/tag/projection/`) and
  `OnTagCreatedFillRecipeViewPart`: insert `household_id` from the event.

### 4c. Ingredient
Same as Tag: `Ingredient`, `IngredientCreated` (+ `householdId`),
`IngredientChanged` (no change), repository finders
(`findByHouseholdAndName`, `getAllById`), DBO, `CreateIngredientCommand` (+ optional
`householdId`), `UpdateIngredientCommandHandler` (ownership check → 404 like recipe),
projections `OnIngredientCreatedFillSummary`, `OnIngredientCreatedFillRecipeViewPart`
insert `household_id`.

### 4d. Plan
- `model/plan/Plan.java` + `PlanGenerated` event: add `householdId`.
- `PlanDbo` + repository mapping.
- `GeneratePlanCommand`: add optional `@UUID public String householdId;`.
- `GeneratePlanCommandHandler`: resolve target household; pass it to
  `findIdsByTagFilterRequirement(requirement, householdId)` so generated plans only
  pick recipes **from the plan's own household** (deliberate: a plan and its recipes
  live together); pass to `Plan.factory().create(...)`.
- Plan projections (in `query/plan/projection/`): insert `household_id` into
  `view_plan_search` and `view_plans` from the event.

### 4e. Shared helper
New `src/main/java/nimnamfood/service/TargetHousehold.java` (static util):
```java
public static UUID resolve(HouseholdScoped command, String requestedHouseholdId) {
    final UserContext context = command.userContext();
    if (requestedHouseholdId == null) {
        return context.personalHouseholdId();
    }
    final UUID householdId = UUID.fromString(requestedHouseholdId);
    if (!context.isMemberOf(householdId)) {
        throw new BusinessError("NOT_A_HOUSEHOLD_MEMBER");
    }
    return householdId;
}
```
(Check `vtertre.ddd.BusinessError`'s constructor signature and adapt; it maps to HTTP
400 via `NimnamfoodExceptionHandler`.)

### 4f. Copy-on-use for tags & ingredients on recipe create/update
In `RecipeService` (already injected in both recipe handlers), add:
```java
public Set<UUID> resolveTagsForHousehold(Set<UUID> tagIds, UUID targetHouseholdId, UserContext context,
                                         List<DomainEvent> collectedEvents)
```
Algorithm (same for ingredients, with `name` copied — ingredient copy also carries
`unit`):
1. Load all referenced tags (`Repositories.tags().getAllById(tagIds)`); if any id is
   missing → `BusinessError("UNKNOWN_TAG")`.
2. For each tag: if `tag.householdId == targetHouseholdId` → keep id. Else if
   `context.isMemberOf(tag.householdId)` → `findByHouseholdAndName(target, tag.name)`;
   if found use its id, otherwise `Tag.factory().create(target, tag.name)`, add it
   via `Repositories.tags().add(...)`, **append the `TagCreated` event to
   `collectedEvents`** (so view projections fill), use the new id. Else →
   `BusinessError("TAG_NOT_ACCESSIBLE")`.
3. Return the substituted id set.

Both recipe command handlers call these before building the aggregate and merge
`collectedEvents` into the returned event list. Note the recipe ingredient parts
carry their own `quantity`/`unit` per recipe — only the ingredient *identity* is
copied.

**Acceptance:** everything compiles including memory repositories;
`./mvnw test` will be red on factory call sites in tests — fix signatures minimally
here (pass `TestHouseholds.HOUSEHOLD_A`), full test overhaul is Phase 6.

## Phase 5 — Query handlers: merged, household-filtered reads

For each handler in `nimnamfood/query/`, add the membership filter using
`query.userContext().householdIds()`:

- `FindRecipesHandler`: both SQL builders gain
  `household_id IN (:householdIds)` — careful: one branch currently has **no WHERE**
  (`SELECT ... FROM view_recipe_search`), one has `WHERE name-filter`, and the CTE
  variant composes `WHERE` from tag clauses. Normalize: always emit
  `WHERE household_id IN (:householdIds)` and append other clauses with `AND`.
  Param: `params.put("householdIds", query.userContext().householdIds())`.
- `GetRecipeHandler`: `... WHERE id = :id AND household_id IN (:householdIds)` (keeps
  existing not-found behavior for other households' recipes).
- `FindTagsHandler`, `FindIngredientsHandler`: add the same IN filter (these back
  autocomplete; returning all households' rows is what enables client-side
  dedupe-by-name).
- `GetPlansHandler`: `WHERE household_id IN (:householdIds)` before ORDER BY.
- `GetPlanHandler`: `id = :id AND household_id IN (:householdIds)`.
- `GetAllIngredientUnitsHandler`: no change (static enum data).

**Acceptance:** compiles; handler tests updated in Phase 6.

## Phase 6 — Tests brought to green + isolation tests

1. **Test helpers** (`src/test/java/nimnamfood/query/**/**TestHelper.java`): every
   INSERT into a view table gains `household_id`; helper methods take a `UUID
   householdId` parameter (or provide overloads defaulting to
   `TestHouseholds.HOUSEHOLD_A`).
2. **Factory call sites** in tests: pass `TestHouseholds.HOUSEHOLD_A` (done partly in
   Phase 4; finish here).
3. **Handler tests**: construct queries with a context, e.g.
   ```java
   FindRecipes query = new FindRecipes();
   query.userContext(new UserContext(TestHouseholds.USER_A, TestHouseholds.HOUSEHOLD_A,
           Set.of(TestHouseholds.HOUSEHOLD_A)));
   ```
   Same for command handler tests.
4. **New isolation tests** (the security-critical ones — one per read handler at
   minimum):
   - Insert a recipe view row in HOUSEHOLD_B; query with a context limited to
     HOUSEHOLD_A → not returned. Same pattern for tags, ingredients, plans.
   - `GetRecipeHandler`/`GetPlanHandler`: row in B, context A → empty/not-found
     result.
   - Context with BOTH households → both households' rows returned (merged view).
   - `UpdateRecipeCommandHandler`: recipe in B, context A →
     `MissingAggregateRootException`.
   - `TargetHousehold`: null → personal; member household → accepted; non-member →
     `BusinessError`.
   - Copy-on-use: recipe created in A referencing a tag of B (user member of both) →
     a tag with the same name exists in A afterwards and the recipe references the A
     tag id; referencing a tag of a household the user is NOT in → `BusinessError`.
   - `UserContextService`: first resolve creates user + personal household +
     membership; second resolve returns the same ids. (Extend
     `PostgresTestContainerBase`; `JdbcClient` is available in the `@DataJdbcTest`
     slice.)

**Acceptance:** `./mvnw test` fully green.

## Phase 7 — Household & user endpoints

House style is command/query bus, but these are thin membership operations with no
projections; implement handlers with `JdbcClient` directly (like projection captors
do) instead of new aggregates — deliberate, documented deviation. All new commands
and queries implement/extend the `HouseholdScoped` contract from Phase 3 (they need
the caller's identity), and resources set the context like everywhere else.

Endpoints (new `web/MeResource.java` and `web/HouseholdsResource.java`):

| Endpoint | Command/Query | Behavior |
|---|---|---|
| `GET /me` | `GetMe` → `MeSummary` | `{ userId, personalHouseholdId, households: [{id, name, type, role}] }` — one SQL join over `households` + `household_members` for `userContext().userId()`. |
| `POST /households` | `CreateHouseholdCommand { @NotBlank name }` → `UUID` | Insert SHARED household + OWNER membership for the caller. |
| `POST /households/{id}/invites` | `CreateHouseholdInviteCommand` → `UUID` token | Caller must be a member (`BusinessError("NOT_A_HOUSEHOLD_MEMBER")` otherwise, checked against `userContext()`); insert invite, `expires_at = now() + interval '7 days'`; return token. Multi-use until expiry. |
| `POST /households/join` | `JoinHouseholdCommand { @UUID token }` → `UUID` householdId | Look up unexpired invite (`expires_at > now()`), else `BusinessError("INVALID_INVITE")`; `INSERT ... ON CONFLICT DO NOTHING` into `household_members` with role MEMBER; return the household id. |

Tests: one `PostgresTestContainerBase` test per handler (create → membership row
exists; join with expired token → error; join twice → idempotent; invite by
non-member → error).

**Acceptance:** `./mvnw test` green.

## Phase 8 — Hardening & finalization

1. New migration `V16__drop_household_id_defaults.sql`: drop the temporary column
   defaults added in V15 (`alter table recipes alter column household_id drop
   default;` for all 12 tables) — from here on, forgetting household_id in an insert
   is a hard DB error rather than silent misattribution.
2. `application.properties`: nothing else; `application-production.properties`:
   confirm `CORS_ALLOWED_ORIGINS` env is set to the real webapp origin(s), never `*`
   (browser + `Authorization` header works with `*` only because we don't use
   credentials, but be strict anyway).
3. README: update the "Running locally" section — Clerk env vars
   (`CLERK_ISSUER_URI`, `INITIAL_CLERK_USER_ID`), how to obtain a test token (Clerk
   dashboard or the webapp's network tab).
4. Grep-based final sweep (the invariants below).

## Invariants — final checklist (verify each, greppably, before calling it done)

- [ ] Every table in {recipes, ingredients, tags, plans, view_tags, view_ingredients,
      view_part_recipe_tags, view_part_recipe_ingredients, view_recipe_search,
      view_recipes, view_plan_search, view_plans} has `household_id uuid not null`,
      in BOTH the Flyway migrations and `src/test/resources/schema.sql`.
- [ ] Every SELECT in `nimnamfood/query/**Handler.java` against a view table filters
      by `household_id` (IN-memberships for lists, AND-clause for by-id). Exception:
      `GetAllIngredientUnitsHandler`.
- [ ] Every INSERT in `nimnamfood/query/**/projection/*.java` writes `household_id`.
- [ ] Every command handler either resolves a target household via
      `TargetHousehold.resolve` (creates) or enforces membership on the loaded
      aggregate with a 404-style `MissingAggregateRootException` (updates).
- [ ] Every command/query class implements/extends the `HouseholdScoped` contract,
      every resource method sets `userContext(...)` before dispatch, and both
      `UserContextRequiredMiddleware`s are registered `@Component @Order(0)`.
- [ ] `userContext` fields on commands are `@JsonIgnore`.
- [ ] `GET /` is the only `permitAll` route.
- [ ] `unique(household_id, name)` on tags and ingredients; the old `unique(name)`
      constraints are gone.
- [ ] `./mvnw test` green, and the isolation tests from Phase 6 exist.

## Out of scope here (tracked separately)

- **nimnamfood-webapp** (Angular 20, separate repo): integrate ClerkJS (community
  `ngx-clerk` or vanilla), HTTP interceptor adding `Authorization: Bearer <token>`,
  sign-in route + route guards, household picker on create forms (default
  "Personnel"), tag-chip dedupe by name (group ids per name → `hasOneOf` filter
  param), household management UI (create/invite/join via the Phase 7 endpoints).
- **nimnamfood-chrome-extension**: same token concern, later.
- Clerk Billing / subscriptions: nothing to do now; the household tables are the
  future billing anchor.
- Tag rename propagation across households, household leave/removal flows, transfer
  of ownership: deliberately deferred.

## Deployment order (Vincent)

1. Create the Clerk app; set `CLERK_ISSUER_URI` + `INITIAL_CLERK_USER_ID` (your
   `user_xxx` id) in the production environment.
2. Deploy the API (migrations backfill everything into your household).
3. Deploy the webapp with the Clerk integration at the same time — old webapp
   versions get 401s once the API is live, so this is a coordinated release.
