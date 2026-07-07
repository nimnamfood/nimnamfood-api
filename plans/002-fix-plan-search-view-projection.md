# Plan 002: Make newly created plans appear in GET /plans (fix view_plan_search projection gap)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java src/test/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummaryTest.java src/main/resources/db/migration/postgresql src/test/resources/schema.sql`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (001 recommended first, not required)
- **Category**: bug
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

`GET /plans` (the plan list endpoint) reads from the `view_plan_search` table.
That table was created and populated **once** by Flyway migration V12, copying
the plans that existed at migration time. Nothing writes to it afterwards: the
only projection reacting to `PlanCreated` (`OnPlanCreatedFillSummary`) inserts
into `view_plans` (the by-id detail view) but not into `view_plan_search`.
Consequence: **every plan generated after migration V12 is permanently invisible
in the plan list**, while `GET /plans/{id}` works. The projection's test didn't
catch this because the list-handler test seeds `view_plan_search` directly via a
test helper. This plan fixes the projection, backfills existing production rows
with a new migration, and adds a regression test.

## Current state

- `src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java`
  — the only `EventCaptor<PlanCreated>` in the codebase. Its `execute` ends
  with (lines 72-75):

```java
client.sql("INSERT INTO view_plans (id, meals) VALUES (:id, :meals::jsonb)")
        .param("id", event.id())
        .param("meals", mealsJson)
        .update();
```

  `client` is a Spring `JdbcClient`. `EventCaptor.execute` is `@Transactional`
  (declared on the interface `vtertre.ddd.event.EventCaptor`), so both inserts
  below will share one transaction.

- The event carries the creation timestamp:
  `src/main/java/nimnamfood/model/plan/PlanCreated.java`:

```java
public record PlanCreated(UUID id, Instant createdAt, ImmutableSet<Meal> meals) implements DomainEvent {
}
```

- The reader: `src/main/java/nimnamfood/query/plan/GetPlansHandler.java:17`:

```java
final var sql = "SELECT id, created_at FROM view_plan_search ORDER BY created_at DESC";
```

- The table: `src/main/resources/db/migration/postgresql/V12__create_plan_search_view.sql`
  creates `view_plan_search (id uuid primary key, created_at timestamp not null)`
  and copies existing rows from `plans`. Migration V13 then altered
  `created_at` to `timestamptz`.

- Timestamp convention for writing `Instant` into a `timestamptz` column in
  this repo: convert with `.atOffset(ZoneOffset.UTC)`. Exemplar —
  `src/test/java/nimnamfood/query/plan/PlanSearchViewTestHelper.java:24`:

```java
Map.of("id", p.getId(), "createdAt", p.createdAt().atOffset(ZoneOffset.UTC))
```

- Existing projection test:
  `src/test/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummaryTest.java`
  — extends `PostgresTestContainerBase` (Testcontainers, requires Docker),
  builds the captor by hand with an injected `JdbcClient`, executes it with a
  `PlanCreated` event, and asserts on the view content. Follow this file's
  structure for the new assertions.

- Migrations: latest is `V13__replace_timestamp_with_timestamptz.sql`. New
  migrations go in `src/main/resources/db/migration/postgresql/` and follow the
  `V<N>__snake_case_description.sql` naming.

- The integration tests do NOT run Flyway. They initialize the schema from
  `src/test/resources/schema.sql` (`spring.sql.init.mode=always` in
  `src/test/resources/application.properties`). `view_plan_search` already
  exists there. Plan 003 changes this test setup; if plan 003 already landed,
  the new migration runs in tests automatically — either way this plan works.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw -q compile` | exit 0 |
| Projection test (needs Docker) | `./mvnw test -Dtest=OnPlanCreatedFillSummaryTest` | BUILD SUCCESS |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |

## Scope

**In scope** (the only files you should modify/create):
- `src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java`
- `src/test/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummaryTest.java`
- `src/main/resources/db/migration/postgresql/V14__backfill_plan_search_view.sql` (create; if a V14 already exists, use the next free number)

**Out of scope** (do NOT touch, even though they look related):
- `GetPlansHandler.java` and `PlansResource.java` — the read path is correct.
- `view_plans` handling or the meals JSON building — unchanged.
- `PlanSearchViewTestHelper.java` — the direct-seed helper is still legitimate
  for handler tests.
- Do not add an `OnPlanCreated...` second captor class; keep both inserts in
  the existing captor so they share one transaction.

## Git workflow

- Branch: `advisor/002-fix-plan-search-view-projection`
- Commit style: French imperative, lowercase (e.g. existing `00b9930 créer et utiliser la vue de récupération des plans`). Suggested: `alimenter la vue de recherche de plans à la création`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Insert into view_plan_search in the projection

In `OnPlanCreatedFillSummary.execute`, immediately after the existing
`view_plans` insert, add:

```java
client.sql("INSERT INTO view_plan_search (id, created_at) VALUES (:id, :createdAt)")
        .param("id", event.id())
        .param("createdAt", event.createdAt().atOffset(ZoneOffset.UTC))
        .update();
```

Add the `java.time.ZoneOffset` import.

**Verify**: `./mvnw -q compile` → exit 0

### Step 2: Regression test

In `OnPlanCreatedFillSummaryTest`, extend both existing tests (or add one new
test) to assert the search-view row exists after `captor.execute(event)`.
Inject `NamedParameterJdbcTemplate` (see `GetPlansHandlerTest` for the
`@Autowired NamedParameterJdbcTemplate template;` pattern) and assert:

```java
var createdAt = template.queryForObject(
        "select created_at from view_plan_search where id = :id",
        Map.of("id", event.id()), java.time.OffsetDateTime.class);
assertThat(createdAt.toInstant()).isCloseTo(event.createdAt(), within(1, ChronoUnit.MICROS));
```

The `isCloseTo ... within(1, ChronoUnit.MICROS)` idiom is the repo convention
for DB timestamp comparison (Postgres stores microseconds) — see
`GetPlansHandlerTest.java:45`.

**Verify**: `./mvnw test -Dtest=OnPlanCreatedFillSummaryTest` → BUILD SUCCESS, 0 failures

### Step 3: Backfill migration for rows created since V12

Create `src/main/resources/db/migration/postgresql/V14__backfill_plan_search_view.sql`:

```sql
insert into view_plan_search (id, created_at)
select id, created_at
from plans
where id not in (select id from view_plan_search);
```

This is idempotent against the state it runs on and repairs production data
(plans created while the bug was live).

**Verify**: `./mvnw verify` → BUILD SUCCESS (note: the default test setup does
not execute Flyway migrations — see plan 003 — so this migration is verified
by review and by local run; if plan 003 has landed, `./mvnw verify` executes it)

### Step 4: Optional local end-to-end check (only if Docker + local DB are available)

Start the DB (`docker compose up -d`) and the app (`./mvnw spring-boot:run`),
then:

```bash
curl -s -X POST localhost:8080/plans/generate -H 'Content-Type: application/json' \
  -d '{"globalTagFilters":{"has":[],"doesNotHave":[],"hasOneOf":[]},"meals":[{"mealIndex":0,"tagFilters":{"has":[],"doesNotHave":[],"hasOneOf":[]}}]}'
curl -s localhost:8080/plans
```

**Verify**: the id returned by the POST appears in the GET response array.
(Projections are async; if the list is momentarily empty, retry the GET once.)
If you cannot run the app, skip this step — Steps 2-3 are the required gates.

## Test plan

- Modified: `OnPlanCreatedFillSummaryTest` — both existing tests gain a
  `view_plan_search` assertion (plan with a meal, plan with empty meals);
  timestamp compared with `isCloseTo`.
- Pattern: existing tests in the same file + `GetPlansHandlerTest` for the
  template-injection and timestamp idioms.
- Verification: `./mvnw test -Dtest='OnPlanCreatedFillSummaryTest,GetPlansHandlerTest'` → all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -n "view_plan_search" src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java` returns 1 match
- [ ] `./mvnw test -Dtest=OnPlanCreatedFillSummaryTest` exits 0
- [ ] `ls src/main/resources/db/migration/postgresql/ | grep -c backfill_plan_search` returns 1
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- A second captor for `PlanCreated` exists (search:
  `grep -rn "EventCaptor<PlanCreated>" src/main/java`) — the single-captor
  assumption would be false.
- `view_plan_search` has columns other than `(id, created_at)` in the live
  schema/migrations — the insert shape would be wrong.
- A migration numbered V14 already exists with different content **and** the
  next free number conflicts with another in-flight plan — report, don't guess.

## Maintenance notes

- If plan deletion is ever added (see rejected/deferred findings in
  `plans/README.md`), both `view_plans` AND `view_plan_search` need delete
  projections — this bug class (one view updated, the sibling forgotten) is
  exactly what to watch for in review.
- The V12 initial-copy + projection-insert pattern means every future view
  table needs its projection wired the same day the table is created; reviewer
  should check any new `view_*` migration for a matching captor.
