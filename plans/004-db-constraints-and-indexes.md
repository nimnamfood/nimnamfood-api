# Plan 004: Add missing database constraints and indexes

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/resources/db/migration src/main/java/nimnamfood/infrastructure/repository/jdbc`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: MED (touches live schema; constraint re-add could fail on dirty data)
- **Depends on**: 001 (only for migration numbering — 001 takes V14)
- **Category**: perf
- **Planned at**: commit `ad32e0f`, 2026-07-07

## Why this matters

Three schema gaps:

1. **`recipe_tags` has no primary key and no index at all.** The recipe tag-filter query (`RecipeJdbcRepository.findIdsByTagFilterRequirement`) runs correlated subqueries against it per recipe row — sequential scans all the way. The missing uniqueness also means duplicate `(recipe_id, tag_id)` rows are possible, which would corrupt the `COUNT(DISTINCT ...)`-based required-tags logic's assumptions.
2. **`meals` lost its `unique (plan_id, meal_index)` constraint** in migration V11, so the domain invariant `DUPLICATE_MEAL_INDEX` (enforced in `Plan.Factory.create`) has no database backstop.
3. **`view_plan_search` has no index on `created_at`**, which `GET /plans` orders by on every call.

## Current state

Relevant files (all read-only references except the new migration):

- `src/main/resources/db/migration/postgresql/V1__initial_entities.sql` — creates `recipe_tags` with no PK:

```sql
create table recipe_tags (
    recipe_id uuid not null,
    tag_id uuid not null
);
```

- `src/main/resources/db/migration/postgresql/V10__add_plan_tables_and_views.sql` — created `meals` with `constraint uk_plan_index unique (plan_id, meal_index)`.
- `src/main/resources/db/migration/postgresql/V11__allow_null_recipe_id_in_meals.sql` — dropped it:

```sql
alter table meals alter column recipe_id drop not null;
alter table meals drop constraint uk_plan_index;
```

- `src/main/resources/db/migration/postgresql/V12__create_plan_search_view.sql` — `view_plan_search (id uuid primary key, created_at timestamp not null)` (type later changed to `timestamptz` by V13).
- `src/main/java/nimnamfood/infrastructure/repository/jdbc/recipe/RecipeJdbcRepository.java` — the query that benefits; e.g. the required-tags clause:

```java
clauses.add("(SELECT COUNT(DISTINCT rt.tag_id) FROM recipe_tags rt WHERE rt.recipe_id = r.id AND rt.tag_id IN (:required)) = :requiredCount");
```

- Persistence context: recipes and plans are saved through Spring Data JDBC (`JdbcAggregateTemplate`), which **deletes and re-inserts child rows** (`recipe_tags`, `meals`) inside one transaction on aggregate save. Delete-then-insert within a transaction cannot violate the new constraints. `RecipeDbo.tags` is a `Set<RecipeTagDbo>`, so duplicates are not produced by the application.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Full suite | `./mvnw test` | `BUILD SUCCESS` (Docker required — Testcontainers; Flyway runs all migrations against a fresh Postgres, which validates the new migration) |
| Focused | `./mvnw test -Dtest='PlanJdbcRepositoryTest,RecipeJdbcRepositoryTest*'` | `BUILD SUCCESS` (if the recipe repo test class name differs, run the whole `infrastructure` package) |

## Scope

**In scope**:
- `src/main/resources/db/migration/postgresql/V15__add_missing_constraints_and_indexes.sql` (create)
- `plans/README.md` (status update)

**Out of scope**:
- Any existing migration file — immutable once applied.
- Java code — no application change is needed for these constraints.
- A GIN index on `view_plans.meals` — the jsonb-scan problem is being removed structurally by plan 007 instead.

## Git workflow

- Commit message style is **French, lowercase, imperative**. Suggested: `ajouter les contraintes et index manquants`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Write the migration

Create `src/main/resources/db/migration/postgresql/V15__add_missing_constraints_and_indexes.sql`. Plan 001 takes `V14`; if V15 is already used, take the next free number and note it in your report.

```sql
-- deduplicate recipe_tags before adding the primary key
delete from recipe_tags a
    using recipe_tags b
where a.ctid < b.ctid
  and a.recipe_id = b.recipe_id
  and a.tag_id = b.tag_id;

alter table recipe_tags
    add primary key (recipe_id, tag_id);

-- restore the meal-index invariant dropped in V11 (the domain also enforces it: DUPLICATE_MEAL_INDEX)
alter table meals
    add constraint uk_plan_index unique (plan_id, meal_index);

-- GET /plans orders by created_at desc on every call
create index idx_view_plan_search_created_at
    on view_plan_search (created_at desc);
```

Notes for you:
- The PK on `(recipe_id, tag_id)` also serves as the index for the `rt.recipe_id = r.id` correlated lookups (leading column `recipe_id`).
- `ctid`-based dedup is standard Postgres for tables without a key.

**Verify**: `ls src/main/resources/db/migration/postgresql/ | sort` → one file per version, no duplicate version numbers.

### Step 2: Run the full suite

Testcontainers spins up a fresh Postgres and Flyway applies V1→V15, so a syntactically or semantically broken migration fails the build immediately.

**Verify**: `./mvnw test` → `BUILD SUCCESS`.

If any test fails with a constraint violation on `meals` (`uk_plan_index`) or `recipe_tags` (duplicate key), this is a STOP condition — it means the application actually produces duplicates and the V11 drop was load-bearing.

## Test plan

No new Java tests: the constraints are exercised by every existing repository test (`PlanJdbcRepositoryTest`, recipe repository tests, all projection tests), and Flyway application on a fresh container is itself the migration test. If you want one explicit guard, you may add a test to `PlanJdbcRepositoryTest` asserting that inserting two meals with the same `(plan_id, meal_index)` via raw SQL throws — optional, not required for done.

## Done criteria

- [ ] `./mvnw test` exits `BUILD SUCCESS`
- [ ] Migration file exists and contains the three statements (dedup+PK, `uk_plan_index`, `idx_view_plan_search_created_at`)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back if:

- Any existing test fails with `duplicate key value violates unique constraint` after adding the migration — the V11 constraint drop may have been deliberate for a reason the audit didn't find (e.g. a save-order issue in Spring Data JDBC). Report the failing test; do not weaken the constraint to a partial index on your own initiative.
- V15 and V16 are both already taken (numbering conflict with other plans) — coordinate with the operator.

## Maintenance notes

- **Production deploy note for the reviewer**: the dedup `DELETE` and `ALTER TABLE ... ADD PRIMARY KEY` take an exclusive lock on `recipe_tags`. At this project's scale that is instantaneous, but it should be mentioned in the PR.
- If meal reordering/editing of persisted plans is ever implemented, Spring Data JDBC's delete-and-reinsert save keeps `uk_plan_index` safe; only raw-SQL partial updates could trip it.
- Plan 007 adds another migration (`view_part_recipe_summary`); keep version numbers coordinated.
