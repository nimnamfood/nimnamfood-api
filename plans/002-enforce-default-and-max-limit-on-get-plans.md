# Plan 002: Enforce the default and maximum page size on `GET /plans`

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/web/PlansResource.java src/test/java/nimnamfood/web`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `ad32e0f`, 2026-07-07

## Why this matters

`PlansResource` defines `MAX_SEARCH_RESULT = 10`, but when the client sends **no** `limit` parameter, the computed limit is `0` — and the framework's `QueryHandlerJdbc.baseLimitOffsetParams` maps `limit <= 0` to SQL `LIMIT NULL`, which in Postgres means **unlimited**. So the intended cap only applies when the client explicitly sends a limit; the default (and `limit=0`) return the entire table. This plan makes the default page size equal to the maximum and clamps explicit values into `[1, 10]`.

## Current state

Relevant files:

- `src/main/java/nimnamfood/web/PlansResource.java` — the endpoint with the broken default.
- `src/main/java/vtertre/query/QueryHandlerJdbc.java` — framework class explaining *why* 0 means unlimited (read-only reference, do not modify).
- `src/main/java/vtertre/query/Query.java` — `limit(int)` / `skip(int)` builder methods on queries (read-only reference).

The bug (`PlansResource.java`, `get` method):

```java
private final int MAX_SEARCH_RESULT = 10;
...
@GetMapping("/plans")
public Future<List<PlanSearchSummary>> get(
        @RequestParam(required = false) Integer skip,
        @RequestParam(required = false) Integer limit) {
    final var computedSkip = Optional.ofNullable(skip)
            .map(value -> Math.max(value, 0))
            .orElse(0);

    final var computedLimit = Optional.ofNullable(limit)
            .map(value -> Math.clamp(value, 0, MAX_SEARCH_RESULT))
            .orElse(0);

    return this.queryBus.dispatch(new GetPlans().skip(computedSkip).limit(computedLimit));
}
```

Why 0 = unlimited (`QueryHandlerJdbc.java`, `baseLimitOffsetParams`):

```java
put("limit", query.limit() > 0 ? query.limit() : null);   // LIMIT NULL = no limit in Postgres
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw test-compile` | `BUILD SUCCESS` |
| One test class | `./mvnw test -Dtest=PlansResourceTest` | `BUILD SUCCESS` |
| Full suite | `./mvnw test` | `BUILD SUCCESS` (Docker must be running — Testcontainers) |

## Scope

**In scope** (the only files you should create or modify):
- `src/main/java/nimnamfood/web/PlansResource.java`
- `src/test/java/nimnamfood/web/PlansResourceTest.java` (create)
- `plans/README.md` (status update)

**Out of scope** (do NOT touch):
- `vtertre/query/QueryHandlerJdbc.java` and `vtertre/query/Query.java` — changing the framework's 0-means-unlimited convention would affect every other endpoint.
- `GetPlansHandler.java` / `GetPlans.java` — the fix belongs at the web boundary.
- `RecipesResource.java` — it may have the same flaw; flag it in your report but do not change it (out of this plan's scope).

## Git workflow

- Commit message style is **French, lowercase, imperative** (see `git log --oneline`). Suggested: `appliquer la limite par défaut et maximale sur la recherche de plans`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Fix the computed limit

In `PlansResource.get`, replace the `computedLimit` expression with:

```java
final var computedLimit = Optional.ofNullable(limit)
        .map(value -> Math.clamp(value, 1, MAX_SEARCH_RESULT))
        .orElse(MAX_SEARCH_RESULT);
```

Semantics after the change: absent → 10; `limit=3` → 3; `limit=0` or negative → 1; `limit=50` → 10. Leave `computedSkip` untouched.

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 2: Write the unit test

Create `src/test/java/nimnamfood/web/PlansResourceTest.java`. There is no existing resource test to copy; write a plain JUnit 5 + Mockito unit test (both are on the test classpath via `spring-boot-starter-test`) — no Spring context, no Testcontainers. Mock both constructor dependencies (`CommandBus`, `QueryBus`), capture the dispatched query with `ArgumentCaptor<GetPlans>`, and assert on `query.limit()` and `query.skip()` (both have public accessors on `vtertre.query.Query`).

Make the mocked `queryBus.dispatch(any())` return `CompletableFuture.completedFuture(List.of())` so the method under test completes.

Cases:

1. `defaultsLimitToMaximumWhenAbsent` — `resource.get(null, null)` → dispatched query has `limit() == 10`, `skip() == 0`.
2. `capsLimitAtMaximum` — `resource.get(null, 50)` → `limit() == 10`.
3. `flooredAtOne` — `resource.get(null, 0)` → `limit() == 1`.
4. `passesValidLimitThrough` — `resource.get(5, 3)` → `limit() == 3`, `skip() == 5`.
5. `flooredSkipAtZero` — `resource.get(-4, null)` → `skip() == 0`.

**Verify**: `./mvnw test -Dtest=PlansResourceTest` → `BUILD SUCCESS`, 5 tests pass.

### Step 3: Run the full suite

**Verify**: `./mvnw test` → `BUILD SUCCESS`.

## Test plan

Covered by Step 2. For Mockito usage style in this repo, see `src/test/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummaryTest.java` (`Mockito.mock()`, `Mockito.when(...)`).

## Done criteria

- [ ] `./mvnw test` exits with `BUILD SUCCESS`; `PlansResourceTest` exists with the 5 cases above
- [ ] `grep -n "orElse(0)" src/main/java/nimnamfood/web/PlansResource.java` matches only the `computedSkip` line (one match)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back if:

- `PlansResource.get` no longer matches the excerpt above (drift).
- `Query.limit()` / `Query.skip()` accessors are absent — the framework changed and the test approach needs revisiting.
- Any existing test starts failing because of this change — that would mean something depended on the unlimited default.

## Maintenance notes

- `RecipesResource` likely has the same absent-limit behavior — worth a follow-up check (deliberately out of scope here).
- If the frontend paginates plans, it must now page with `skip`/`limit`; the API will no longer return everything by default. Mention this in the PR description.
- If a "return everything" admin need appears later, add an explicit parameter rather than restoring 0-means-unlimited.
