# Plan 004: Add an HTTP-layer test baseline for all REST resources and the exception handler

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/web src/main/java/nimnamfood/NimnamfoodExceptionHandler.java`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: tests
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

The 10 REST controllers in `src/main/java/nimnamfood/web/` and the
`@ControllerAdvice` exception handler have **zero tests** — no `@WebMvcTest`,
no MockMvc, nothing exercises HTTP serialization, status codes, the async
`Future` return values, request-param handling, or exception-to-status mapping.
Handlers and repositories are well tested, but the boundary the frontend
actually talks to is not. These tests also become the safety net for plan 005
(authentication), which modifies exactly this layer — land this first.

## Current state

- Controllers: `src/main/java/nimnamfood/web/*Resource.java` — 10 classes, all
  `@RestController`, all returning `Future<...>` from bus dispatches. Full
  endpoint inventory:
  - `HealthResource` — `GET /`
  - `RecipesResource` — `POST /recipes` (201, body `{"id": <uuid>}`), `GET /recipes?q=&tags=&skip=&limit=`
  - `RecipeResource` — `GET /recipes/{stringUuid}`, `PUT /recipes/{stringUuid}` (204)
  - `IngredientsResource` — `POST /ingredients` (201), `GET /ingredients`
  - `IngredientResource` — `PUT /ingredients/{stringUuid}`
  - `IngredientUnitsResource` — `GET /units`
  - `TagsResource` — `GET /tags`, `POST /tags` (201)
  - `PlansResource` — `POST /plans/generate` (201), `GET /plans?skip=&limit=`
  - `PlanResource` — `GET /plans/{stringUuid}`
  - `IllustrationsResource` — `POST /illustrations` (multipart, 201)

- Representative controller shape (`RecipesResource.java:33-38`):

```java
@PostMapping("/recipes")
public Future<ResponseEntity<Map<String, UUID>>> create(@RequestBody CreateRecipeCommand command) {
    return this.commandBus.dispatch(command)
            .thenApply(result -> Collections.singletonMap("id", result))
            .thenApply(result -> new ResponseEntity<>(result, HttpStatus.CREATED));
}
```

  Controllers receive `vtertre.command.CommandBus` and/or
  `vtertre.query.QueryBus` via constructor injection. `dispatch` returns
  `CompletableFuture`.

- `src/main/java/nimnamfood/NimnamfoodExceptionHandler.java` — `@ControllerAdvice`
  mapping: `ValidationException` → 400 `{"errors": [...]}`;
  `MissingAggregateRootException` → 404 `{"error": msg}`;
  `DuplicateKeyException` → 400 `{"error": "DUPLICATE_IDENTIFIER"}`;
  `BusinessError` → 400 `{"error": msg}`. Nothing else is mapped
  (e.g. `FailedToUploadIllustrationException` and `IllegalArgumentException`
  from `UUID.fromString` fall through to Spring's default 500).

- `nimnamfood.web.converter.StringToTagFilterQueryConverter` is a `@Component`
  `Converter<String, TagFilterQuery>` — `@WebMvcTest` slices include
  `Converter` beans, so `GET /recipes?tags=...` works in the slice.

- The buses are NOT auto-configured in a `@WebMvcTest` slice (they are `@Bean`s
  in `NimnamfoodConfiguration`, which slices don't load) — mock them with
  `@MockBean CommandBus commandBus;` / `@MockBean QueryBus queryBus;`.

- Async MockMvc idiom (controllers return futures, so a request needs a second
  dispatch):

```java
var mvcResult = mockMvc.perform(post("/recipes").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(request().asyncStarted())
        .andReturn();
mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()));
```

  For error cases, make the mocked bus return
  `CompletableFuture.failedFuture(new MissingAggregateRootException(id))`.
  Spring's `DeferredResult` adapter unwraps `CompletionException`, so the
  `@ExceptionHandler` methods should receive the original exception — the tests
  below pin that behavior down.

- Existing test conventions: JUnit 5, AssertJ, Mockito available via
  `spring-boot-starter-test` (see `OnPlanCreatedFillSummaryTest` for
  `Mockito.mock()` usage). No HTTP test exists yet, so this plan sets the
  pattern. These tests need no Docker.

- Command classes (e.g. `CreateRecipeCommand`, `GeneratePlanCommand`) have
  public fields and are deserialized by Jackson; build request JSON by hand in
  the tests, no builders needed.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile tests | `./mvnw -q test-compile` | exit 0 |
| One test class (no Docker needed) | `./mvnw test -Dtest=RecipesResourceTest` | BUILD SUCCESS |
| All new tests | `./mvnw test -Dtest='*ResourceTest'` | BUILD SUCCESS |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |

## Scope

**In scope** (create only; do not modify production code):
- `src/test/java/nimnamfood/web/RecipesResourceTest.java`
- `src/test/java/nimnamfood/web/RecipeResourceTest.java`
- `src/test/java/nimnamfood/web/PlansResourceTest.java`
- `src/test/java/nimnamfood/web/PlanResourceTest.java`
- `src/test/java/nimnamfood/web/IllustrationsResourceTest.java`
- `src/test/java/nimnamfood/web/IngredientsResourceTest.java`
- `src/test/java/nimnamfood/web/IngredientResourceTest.java`
- `src/test/java/nimnamfood/web/IngredientUnitsResourceTest.java`
- `src/test/java/nimnamfood/web/TagsResourceTest.java`
- `src/test/java/nimnamfood/web/HealthResourceTest.java`

**Out of scope** (do NOT touch, even though they look related):
- All production code, including `NimnamfoodExceptionHandler.java`. If a test
  reveals wrong behavior (e.g. a 500 where a 400 belongs), **pin the current
  behavior with the test and record the discrepancy in your final report** —
  fixing it is a separate decision.
- `CorsConfiguration` — CORS behavior is covered by plan 005.
- No `@SpringBootTest` full-context tests — slice tests only; the full wiring
  is exercised by the Testcontainers suite.

## Git workflow

- Branch: `advisor/004-http-layer-test-baseline`
- Commit style: French imperative, lowercase. Suggested: `ajouter des tests de la couche http`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Establish the pattern with RecipesResourceTest

Create `src/test/java/nimnamfood/web/RecipesResourceTest.java`:

```java
@WebMvcTest(RecipesResource.class)
class RecipesResourceTest {
    @Autowired MockMvc mockMvc;
    @MockBean CommandBus commandBus;
    @MockBean QueryBus queryBus;
    ...
}
```

Tests: (a) `POST /recipes` happy path → 201 + `$.id`; (b) `GET /recipes` →
200 + JSON array (mock `queryBus.dispatch(any())` to return a completed future
with one `RecipeSearchSummary`); (c) `GET /recipes?tags=a,!b` → 200 (converter
path); (d) `POST /recipes` where the bus future fails with
`ValidationException` → 400 with `$.errors` array; (e) failed future with
`DuplicateKeyException` → 400 with `$.error` = `DUPLICATE_IDENTIFIER`.
Check the constructor signature of `ValidationException`
(`src/main/java/vtertre/command/ValidationException.java`) and of the summary
records before writing.

**Verify**: `./mvnw test -Dtest=RecipesResourceTest` → BUILD SUCCESS

### Step 2: Exception-handler coverage via RecipeResourceTest

`GET /recipes/{uuid}` with the bus failing with
`MissingAggregateRootException` → 404 + `$.error`; failing with
`BusinessError("X")` → 400 + `$.error` = "X"; `PUT /recipes/{uuid}` happy path
→ 204. Also add one test documenting current behavior for a malformed UUID in
the path (`PUT /recipes/not-a-uuid` — `UUID.fromString` throws
`IllegalArgumentException`; expected today: 500). Whatever status you observe,
assert it and flag it in your report.

Together with Step 1, all four `@ExceptionHandler` mappings in
`NimnamfoodExceptionHandler` are now covered.

**Verify**: `./mvnw test -Dtest=RecipeResourceTest` → BUILD SUCCESS

### Step 3: Remaining resources

One test class each, same pattern, smaller scope:
- `PlansResourceTest`: POST generate → 201 + id; GET with `limit=50` → verify
  the query dispatched has limit clamped to 10 (capture with
  `ArgumentCaptor<GetPlans>`); GET without params → 200.
- `PlanResourceTest`: GET happy path → 200; missing plan → 404.
- `IllustrationsResourceTest`: multipart POST (`MockMultipartFile`, name
  `"file"`) → 201 + id.
- `IngredientsResourceTest`, `IngredientResourceTest`, `TagsResourceTest`:
  happy path per endpoint + one error mapping each.
- `IngredientUnitsResourceTest`, `HealthResourceTest`: single happy-path test.
  (`HealthResource` returns no Future — plain request, no asyncDispatch.)

**Verify**: `./mvnw test -Dtest='*ResourceTest'` → BUILD SUCCESS, ~25+ tests

### Step 4: Full suite

**Verify**: `./mvnw verify` → BUILD SUCCESS (Docker required)

## Test plan

This plan IS the test plan. Coverage contract: every endpoint listed in
"Current state" has at least one happy-path test; each of the 4 exception
mappings is asserted at least once; the plans `limit` clamp and the tag-filter
converter each have one test.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `ls src/test/java/nimnamfood/web/*ResourceTest.java | wc -l` returns 10
- [ ] `./mvnw test -Dtest='*ResourceTest'` exits 0
- [ ] `grep -rln "MissingAggregateRootException\|ValidationException\|DuplicateKeyException\|BusinessError" src/test/java/nimnamfood/web/ | wc -l` ≥ 2
- [ ] `git status` shows only new files under `src/test/java/nimnamfood/web/` (plus the plans index)
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `@WebMvcTest` cannot start because a controller pulls in a bean that cannot
  be mocked with `@MockBean` (constructor signatures in "Current state" say
  only CommandBus/QueryBus are needed — if that's wrong, the codebase drifted).
- The failed-future error cases return 500 instead of the mapped status for
  ALL exception types — that means `CompletionException` unwrapping doesn't
  happen in this Spring version, which is a production bug worth reporting
  before pinning 10 test files to the wrong expectation.
- You are tempted to modify production code to make a test pass — out of
  scope; report instead.

## Maintenance notes

- Plan 005 (authentication) will make every one of these requests require a
  token; these tests will need an update (adding the auth header or excluding
  the filter from the slice) — that update belongs to plan 005, which is why
  its plan depends on this one.
- Reviewer should scrutinize: tests must assert on `asyncDispatch` results, not
  on `asyncStarted` alone — a test passing without the second dispatch asserts
  nothing about the response.
- Discrepancies discovered while pinning behavior (expected: the malformed-UUID
  500) should land in the PR description as candidate follow-ups.
