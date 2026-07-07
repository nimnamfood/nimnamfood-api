# Plan 001: Fill `view_plan_search` when a plan is created

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/query/plan src/main/resources/db/migration src/test/java/nimnamfood/query/plan`
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

`GET /plans` reads from the `view_plan_search` table, but **nothing in production code ever writes to that table**. The Flyway migration `V12__create_plan_search_view.sql` backfilled plans that existed when it ran, and the test helper inserts rows directly — which is why tests pass. Any plan generated since V12 is invisible in the plan list (it is still reachable via `GET /plans/{id}`, because a separate projection fills `view_plans`). This plan adds the missing projection and backfills the rows that were lost.

## Current state

Relevant files:

- `src/main/java/nimnamfood/query/plan/GetPlansHandler.java` — the query handler that reads `view_plan_search` (read-only reference, do not modify).
- `src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java` — the only existing `PlanCreated` captor; it fills `view_plans`, not `view_plan_search`.
- `src/main/java/nimnamfood/query/recipe/projection/OnRecipeCreatedFillSearchSummary.java` — the recipe equivalent of what you are about to write; use it as the structural exemplar.
- `src/main/java/nimnamfood/model/plan/PlanCreated.java` — the event to capture.
- `src/main/resources/db/migration/postgresql/` — Flyway migrations, currently V1–V13.

The event (`src/main/java/nimnamfood/model/plan/PlanCreated.java`):

```java
public record PlanCreated(UUID id, Instant createdAt, ImmutableSet<Meal> meals) implements DomainEvent {
}
```

The target table (created in `V12__create_plan_search_view.sql`, column type changed to `timestamptz` in V13):

```sql
create table view_plan_search
(
    id         uuid primary key,
    created_at timestamp not null
);
```

How the query side reads it (`GetPlansHandler.java:17`):

```java
final var sql = "SELECT id, created_at FROM view_plan_search ORDER BY created_at DESC";
```

The exemplar to follow — captors are `@Component` classes implementing `vtertre.ddd.event.EventCaptor<TEvent>`; Spring auto-collects all `EventCaptor` beans into the event bus (see `NimnamfoodConfiguration.eventBus`), so **no registration step is needed**. From `OnRecipeCreatedFillSearchSummary.java`:

```java
@Component
public class OnRecipeCreatedFillSearchSummary implements EventCaptor<RecipeCreated> {
    private final JdbcClient client;
    ...
    @Override
    public void execute(RecipeCreated event) {
        ...
        this.client.sql(sqlQuery)
                .param("id", event.id())
                ...
                .param("creationDateTime", event.creationDateTime().atOffset(ZoneOffset.UTC))
                ...
                .update();
    }
}
```

Note the `Instant -> OffsetDateTime` conversion via `.atOffset(ZoneOffset.UTC)` when binding a timestamp parameter — match it.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw test-compile` | `BUILD SUCCESS` |
| One test class | `./mvnw test -Dtest=OnPlanCreatedFillSearchSummaryTest` | `BUILD SUCCESS`, tests run > 0 |
| Full suite | `./mvnw test` | `BUILD SUCCESS` |

Tests use Testcontainers — **Docker must be running**. If `./mvnw test` fails with a Docker/Testcontainers connection error, that is an environment problem, not a code problem: report it.

## Scope

**In scope** (the only files you should create or modify):
- `src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSearchSummary.java` (create)
- `src/main/resources/db/migration/postgresql/V14__backfill_plan_search_view.sql` (create)
- `src/test/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSearchSummaryTest.java` (create)
- `plans/README.md` (status update)

**Out of scope** (do NOT touch, even though they look related):
- `OnPlanCreatedFillSummary.java` — the `view_plans` projection is a separate concern (plan 007 touches it).
- `GetPlansHandler.java`, `PlansResource.java` — the default-limit fix is plan 002.
- `PlanSearchViewTestHelper.java` — existing tests keep using it.
- Any existing migration file V1–V13 — Flyway migrations are immutable once applied.

## Git workflow

- Branch: work directly on the current branch unless the operator says otherwise.
- Commit message style is **French, lowercase, imperative**, e.g. `créer et utiliser la vue de récupération des plans`. Suggested message: `alimenter la vue de recherche des plans à la création`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Create the captor

Create `src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSearchSummary.java`:

```java
package nimnamfood.query.plan.projection;

import nimnamfood.model.plan.PlanCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

import java.time.ZoneOffset;

@Component
public class OnPlanCreatedFillSearchSummary implements EventCaptor<PlanCreated> {
    private final JdbcClient client;

    @Autowired
    public OnPlanCreatedFillSearchSummary(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(PlanCreated event) {
        this.client.sql("INSERT INTO view_plan_search (id, created_at) VALUES (:id, :createdAt)")
                .param("id", event.id())
                .param("createdAt", event.createdAt().atOffset(ZoneOffset.UTC))
                .update();
    }
}
```

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 2: Create the backfill migration

Create `src/main/resources/db/migration/postgresql/V14__backfill_plan_search_view.sql`. If a `V14__*.sql` already exists (another plan may have been executed first), use the next free version number instead and mention it in your report.

```sql
insert into view_plan_search (id, created_at)
select p.id, p.created_at
from plans p
on conflict (id) do nothing;
```

The `on conflict` clause makes it idempotent with respect to the rows V12 already copied.

**Verify**: `ls src/main/resources/db/migration/postgresql/ | sort` → exactly one file per version number, V14 (or your chosen number) present.

### Step 3: Write the test

Create `src/test/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSearchSummaryTest.java`, modeled structurally on the existing `src/test/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummaryTest.java` (same package, extends `PostgresTestContainerBase`, constructs the captor manually with the injected `JdbcClient`). This test does not need `WithJdbcRepositories` or a mocked `RecipeService` — the captor only needs `JdbcClient`.

Cases to cover:

1. `insertsASearchSummaryRow` — build a `PlanCreated` event (`new PlanCreated(UUID.randomUUID(), Instant.now(), ImmutableSet.of())`), call `captor.execute(event)`, then read the row back with an injected `NamedParameterJdbcTemplate`:
   - the row with `id = event.id()` exists;
   - `created_at` (read as `OffsetDateTime`, converted with `.toInstant()`) is close to `event.createdAt()` — use `assertThat(...).isCloseTo(event.createdAt(), within(1, ChronoUnit.MICROS))` like `GetPlansHandlerTest` does (Postgres stores microsecond precision).
2. (Optional but nice) an end-to-end check through the existing handler: after `captor.execute(event)`, `new GetPlansHandler().execute(new GetPlans(), template)` returns a list containing the plan id.

**Verify**: `./mvnw test -Dtest=OnPlanCreatedFillSearchSummaryTest` → `BUILD SUCCESS`, your new tests pass.

### Step 4: Run the full suite

**Verify**: `./mvnw test` → `BUILD SUCCESS`, no regressions.

## Test plan

Covered by Step 3. Pattern files: `OnPlanCreatedFillSummaryTest.java` (captor test structure), `GetPlansHandlerTest.java` (timestamp assertion style).

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./mvnw test` exits with `BUILD SUCCESS`
- [ ] `grep -rn "INSERT INTO view_plan_search" src/main/java/` returns exactly one match, in `OnPlanCreatedFillSearchSummary.java`
- [ ] A backfill migration `V14__backfill_plan_search_view.sql` (or documented alternative number) exists
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `view_plan_search` has columns other than `(id, created_at)` when you inspect migrations V12/V13 — the schema drifted.
- A `V14` migration already exists AND V15 is also taken — coordinate numbering with the operator.
- `OnPlanCreatedFillSummaryTest` no longer exists or `PostgresTestContainerBase` cannot be found — the test infrastructure changed.
- The new test fails twice after a reasonable fix attempt.

## Maintenance notes

- The event bus is **asynchronous and fire-and-forget** (see `EventBusAsync`): if this captor throws, the row is silently lost. Plan 003 addresses that (error logging + making `PlanCreated` synchronous). This plan intentionally does not.
- If plans ever become deletable, a matching `OnPlanDeleted...` captor must remove the search row.
- Reviewer should check: the captor binds `created_at` as `OffsetDateTime` (UTC), consistent with `PlanSearchViewTestHelper`.
