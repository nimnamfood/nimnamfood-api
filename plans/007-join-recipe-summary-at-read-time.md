# Plan 007: Stop denormalizing recipe data into plan views — join a shared recipe summary at read time

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/query/plan src/main/java/nimnamfood/query/recipe/projection src/main/resources/db/migration src/test/java/nimnamfood/query/plan`
> Plans 001–004 legitimately touch some of these paths (new search-summary
> captor, migrations V14/V15). Anything else that changed must be compared
> against the "Current state" excerpts; on a mismatch, STOP.

## Status

- **Priority**: P3
- **Effort**: L
- **Risk**: MED (migration transforms existing view data; several files change together)
- **Depends on**: 001 (migration numbering), 004 (migration numbering). Independent of 002/003/005/006, but land after 003 so failures in the simplified projection are observable.
- **Category**: tech-debt (+ perf)
- **Planned at**: commit `ad32e0f`, 2026-07-07

## Why this matters

The plan detail view (`view_plans`) copies each meal's recipe **name and illustration URL** into a jsonb column at projection time. That copy must then be kept fresh: `OnRecipeChangedUpdatePlanSummary` rewrites the jsonb of every affected plan on every recipe change, via a full-table scan with a containment predicate and a hand-built `jsonb_agg`/`jsonb_set` transformation. Every copied field is a standing consistency obligation — and the event pipeline it rides on is fire-and-forget, so a missed update means silently stale plan displays.

The fix: introduce a **shared recipe summary read-model part** (`view_part_recipe_summary`, following the existing `view_part_recipe_tags` convention) owned and maintained by the recipe context, store only `meal_index` + `recipe_id` in `view_plans`, and compose the plan response at read time. The HTTP response shape of `GET /plans/{id}` does not change. This deletes the entire `OnRecipeChangedUpdatePlanSummary` projection and removes the plan context's dependency on the recipe **write model** in `OnPlanCreatedFillSummary`.

## Current state

### Files and roles

- `src/main/resources/db/migration/postgresql/V10__add_plan_tables_and_views.sql` — created `view_plans (id uuid primary key, meals jsonb not null)`.
- `src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java` — fills `view_plans`; currently loads full `Recipe` aggregates via `Repositories.recipes().getAllById(...)` and calls `RecipeService.illustrationUrl(...)` to build `MealSummary` objects with embedded recipe name/url, serialized with the snake_case "Jsonb" `ObjectMapper`.
- `src/main/java/nimnamfood/query/plan/projection/OnRecipeChangedUpdatePlanSummary.java` — the sync-back projection to **delete**; captures `RecipeChanged` and rewrites `view_plans.meals` jsonb.
- `src/main/java/nimnamfood/query/plan/GetPlanHandler.java` — reads `view_plans` and deserializes `meals` into `Set<MealSummary>`:

```java
final String sql = "SELECT id, meals FROM view_plans WHERE id = :planId";

return template.query(sql, new MapSqlParameterSource("planId", query.id), resultSet -> {
    if (!resultSet.next()) {
        throw new MissingAggregateRootException(query.id);
    }
    try {
        return new PlanSummary(
                resultSet.getObject("id", UUID.class),
                mapper.readValue(resultSet.getString("meals"), mealsTypeReference)
        );
    } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
    }
});
```

- `src/main/java/nimnamfood/query/plan/model/` — response records (these define the **unchanged** HTTP shape):

```java
public record PlanSummary(UUID id, Set<MealSummary> meals) {}
public record MealSummary(int mealIndex, MealRecipeSummary recipe) {}
public record MealRecipeSummary(UUID id, String name, String illustrationUrl) {}
```

- `src/main/java/nimnamfood/model/plan/PlanCreated.java` — `record PlanCreated(UUID id, Instant createdAt, ImmutableSet<Meal> meals)`; `Meal` exposes `mealIndex()` and `recipeId()` (nullable).
- `src/main/java/nimnamfood/model/recipe/RecipeCreated.java` / `RecipeChanged.java` — recipe events; both carry `id`, `name`, `illustrationId` (plus fields you won't need).
- `src/main/java/nimnamfood/query/recipe/projection/OnIngredientChangedUpdateRecipeViewPart.java` — exemplar for a captor maintaining a `view_part_*` table:

```java
@Component
public class OnIngredientChangedUpdateRecipeViewPart implements EventCaptor<IngredientChanged> {
    private final JdbcClient client;

    @Autowired
    public OnIngredientChangedUpdateRecipeViewPart(JdbcClient client) { this.client = client; }

    @Override
    public void execute(IngredientChanged event) {
        this.client.sql("UPDATE view_part_recipe_ingredients SET name = :name WHERE id = :id")
                .param("id", event.id()).param("name", event.name()).update();
    }
}
```

- `src/main/java/nimnamfood/service/RecipeService.java` — has `illustrationUrl(UUID illustrationId)` used by every projection that renders illustration URLs; reuse it.
- Migration exemplar for illustration URLs in SQL backfills — `V7__create_recipe_search_view.sql` builds them with a **Flyway placeholder** `${storage-bucket}`:

```sql
'https://firebasestorage.googleapis.com/v0/b/${storage-bucket}/o/live%2Frecipes%2F' || r.illustration_id::text || '.webp?alt=media&token=' || r.illustration_id::text
```

- Tests to update: `src/test/java/nimnamfood/query/plan/PlansViewTestHelper.java` (inserts/reads `view_plans` rows), `GetPlanHandlerTest.java`, `projection/OnPlanCreatedFillSummaryTest.java`, and `projection/OnRecipeChangedUpdatePlanSummaryTest.java` (to delete).
- Serialization convention: the "Jsonb" mapper is snake_case (`JacksonConfiguration.jsonbMapper`; tests build it via `nimnamfood.query.ObjectMapperFactory.withSnakeCasePropertyNamingStrategy()`), so a record field `recipeId` is stored as `recipe_id` in jsonb.

### Current jsonb shape in `view_plans.meals` (what the migration must transform)

```json
[{"meal_index": 0, "recipe": {"id": "…", "name": "…", "illustration_url": "…"}}, {"meal_index": 1, "recipe": null}]
```

### Target jsonb shape

```json
[{"meal_index": 0, "recipe_id": "…"}, {"meal_index": 1, "recipe_id": null}]
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw test-compile` | `BUILD SUCCESS` |
| Focused | `./mvnw test -Dtest='GetPlanHandlerTest,OnPlanCreatedFillSummaryTest,OnRecipeCreatedFillRecipeSummaryPartTest,OnRecipeChangedUpdateRecipeSummaryPartTest'` | `BUILD SUCCESS` |
| Full suite | `./mvnw test` | `BUILD SUCCESS` (Docker required — Testcontainers; Flyway validates the migration on a fresh DB) |

## Scope

**In scope**:
- `src/main/resources/db/migration/postgresql/V16__create_recipe_summary_part_and_slim_plan_view.sql` (create; use next free number if taken)
- `src/main/java/nimnamfood/query/recipe/projection/OnRecipeCreatedFillRecipeSummaryPart.java` (create)
- `src/main/java/nimnamfood/query/recipe/projection/OnRecipeChangedUpdateRecipeSummaryPart.java` (create)
- `src/main/java/nimnamfood/query/plan/model/PlanMealEntry.java` (create)
- `src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java` (simplify)
- `src/main/java/nimnamfood/query/plan/projection/OnRecipeChangedUpdatePlanSummary.java` (delete)
- `src/main/java/nimnamfood/query/plan/GetPlanHandler.java` (rewrite query logic)
- Tests: `PlansViewTestHelper.java`, `GetPlanHandlerTest.java`, `OnPlanCreatedFillSummaryTest.java` (update); `OnRecipeChangedUpdatePlanSummaryTest.java` (delete); `OnRecipeCreatedFillRecipeSummaryPartTest.java`, `OnRecipeChangedUpdateRecipeSummaryPartTest.java` (create)
- `plans/README.md` (status update)

**Out of scope**:
- `PlanSummary` / `MealSummary` / `MealRecipeSummary` records — the response contract must not change.
- `view_recipes` / `view_recipe_search` and their projections — recipe screens keep their own views; `view_part_recipe_summary` is a **new, deliberately shared** table.
- Dropping `view_plans` in favor of reading `plans`/`meals` write tables — considered and deferred (see Maintenance notes).
- `GetPlansHandler` / `view_plan_search` — untouched by this plan.

## Git workflow

- Commit message style is **French, lowercase, imperative**. Suggested: `composer le détail d'un plan avec le résumé de recette partagé`.
- One commit per step is fine; keep the migration and code changes in the same PR (they only work together).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Migration — create the shared summary part and transform `view_plans`

Create `V16__create_recipe_summary_part_and_slim_plan_view.sql` (plans 001/004 take V14/V15; use the next free number if the sequence moved):

```sql
create table view_part_recipe_summary
(
    id               uuid primary key,
    name             text not null,
    illustration_url text null
);

insert into view_part_recipe_summary (id, name, illustration_url)
select r.id,
       r.name,
       case
           when r.illustration_id is not null then
               'https://firebasestorage.googleapis.com/v0/b/${storage-bucket}/o/live%2Frecipes%2F' ||
               r.illustration_id::text || '.webp?alt=media&token=' || r.illustration_id::text
           end
from recipes r;

update view_plans
set meals = coalesce(
        (select jsonb_agg(jsonb_build_object(
                'meal_index', meal -> 'meal_index',
                'recipe_id', meal -> 'recipe' -> 'id'))
         from jsonb_array_elements(meals) as meal),
        '[]'::jsonb);
```

Notes: `meal -> 'recipe' -> 'id'` yields jsonb `null` when `recipe` is null — exactly the target shape. The URL expression is copied from V7 and uses the `${storage-bucket}` Flyway placeholder already configured for this project.

**Verify**: `ls src/main/resources/db/migration/postgresql/ | sort` → no duplicate version numbers.

### Step 2: Recipe-context captors that maintain the part

Create `OnRecipeCreatedFillRecipeSummaryPart.java` and `OnRecipeChangedUpdateRecipeSummaryPart.java` in `nimnamfood.query.recipe.projection`, modeled on `OnIngredientChangedUpdateRecipeViewPart` (excerpt above), both `@Component EventCaptor<...>` with `JdbcClient` + `RecipeService` constructor args:

- `OnRecipeCreatedFillRecipeSummaryPart implements EventCaptor<RecipeCreated>`:
  `INSERT INTO view_part_recipe_summary (id, name, illustration_url) VALUES (:id, :name, :illustrationUrl)`
- `OnRecipeChangedUpdateRecipeSummaryPart implements EventCaptor<RecipeChanged>`:
  `UPDATE view_part_recipe_summary SET name = :name, illustration_url = :illustrationUrl WHERE id = :id`

In both, compute the URL exactly like the existing captors do:

```java
final String illustrationUrl = event.illustrationId() != null
        ? this.recipeService.illustrationUrl(event.illustrationId())
        : null;
```

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 3: New jsonb entry record

Create `src/main/java/nimnamfood/query/plan/model/PlanMealEntry.java`:

```java
package nimnamfood.query.plan.model;

import java.util.UUID;

public record PlanMealEntry(int mealIndex, UUID recipeId) {
}
```

With the snake_case Jsonb mapper this (de)serializes to/from `{"meal_index": 0, "recipe_id": "…"}`.

### Step 4: Simplify `OnPlanCreatedFillSummary`

Rewrite it to depend only on `JdbcClient` and the `@Qualifier("Jsonb") ObjectMapper` (drop `RecipeService` and all `Repositories.recipes()` usage). Body of `execute`:

```java
final Set<PlanMealEntry> entries = event.meals().stream()
        .map(meal -> new PlanMealEntry(meal.mealIndex(), meal.recipeId()))
        .collect(Collectors.toSet());

final String mealsJson;
try {
    mealsJson = mapper.writeValueAsString(entries);
} catch (JsonProcessingException e) {
    throw new RuntimeException(e);
}

client.sql("INSERT INTO view_plans (id, meals) VALUES (:id, :meals::jsonb)")
        .param("id", event.id())
        .param("meals", mealsJson)
        .update();
```

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 5: Delete the sync-back projection

Delete `OnRecipeChangedUpdatePlanSummary.java` and its test `OnRecipeChangedUpdatePlanSummaryTest.java`.

**Verify**: `grep -rn "OnRecipeChangedUpdatePlanSummary" src/` → no matches.

### Step 6: Rewrite `GetPlanHandler`

Keep the class signature (`extends QueryHandlerJdbc<GetPlan, PlanSummary>`, constructor takes the Jsonb mapper) and the `MissingAggregateRootException` on absent plan. New logic in `execute(GetPlan query, NamedParameterJdbcTemplate template)`:

1. Query 1 — `SELECT id, meals FROM view_plans WHERE id = :planId`; if no row → `throw new MissingAggregateRootException(query.id)`. Deserialize `meals` into `Set<PlanMealEntry>` (a `TypeReference<Set<PlanMealEntry>>`).
2. Collect the non-null `recipeId`s. If non-empty, query 2 — `SELECT id, name, illustration_url FROM view_part_recipe_summary WHERE id IN (:recipeIds)` (bind the collection with `MapSqlParameterSource("recipeIds", ids)`), building a `Map<UUID, MealRecipeSummary>`.
3. Compose: each entry becomes `new MealSummary(entry.mealIndex(), summariesById.get(entry.recipeId()))` — which is `null` for meals without a recipe **and** for a recipe id missing from the part table (acceptable: renders like a recipe-less meal rather than failing the whole plan).
4. Return `new PlanSummary(planId, mealSummaries)`.

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 7: Update the test helper and tests

- `PlansViewTestHelper` (test-side mirror of the view): change `insert` to take `UUID planId, Set<PlanMealEntry> entries` and serialize those; change `find(UUID)` to return the raw `Set<PlanMealEntry>` (rename to `findEntries` for clarity). Add a helper `insertRecipeSummary(UUID id, String name, String illustrationUrl)` that inserts into `view_part_recipe_summary` (or put that in a small new helper class — your choice, keep it in `src/test/java/nimnamfood/query/plan/`).
- `GetPlanHandlerTest`: for the happy path, insert a `view_part_recipe_summary` row **and** a `view_plans` row referencing it, then assert the composed `PlanSummary` still carries `mealIndex`, recipe `id`, `name`, `illustrationUrl` (same assertions as today). Keep the not-found test unchanged. Add two cases: meal with `recipeId = null` → `recipe` is null; meal whose `recipe_id` has no summary row → `recipe` is null.
- `OnPlanCreatedFillSummaryTest`: no more `Recipe`/`RecipeService`/`WithJdbcRepositories` needed. Assert the stored entries: execute the captor with a `PlanCreated` containing one meal, then read back via the updated helper and assert `Set.of(new PlanMealEntry(0, recipeId))`. Keep the `mealsCanBeEmpty` case.

**Verify**: `./mvnw test -Dtest='GetPlanHandlerTest,OnPlanCreatedFillSummaryTest'` → `BUILD SUCCESS`.

### Step 8: Tests for the new recipe-summary captors

Create `OnRecipeCreatedFillRecipeSummaryPartTest` and `OnRecipeChangedUpdateRecipeSummaryPartTest` in `src/test/java/nimnamfood/query/recipe/projection/`, modeled on `OnPlanCreatedFillSummaryTest` (extends `PostgresTestContainerBase`, mocked `RecipeService`, captor constructed manually). Cases: insert fills the row (with and without illustration → URL null); update changes name and URL of an existing row.

**Verify**: `./mvnw test -Dtest='OnRecipeCreatedFillRecipeSummaryPartTest,OnRecipeChangedUpdateRecipeSummaryPartTest'` → `BUILD SUCCESS`.

### Step 9: Full suite

**Verify**: `./mvnw test` → `BUILD SUCCESS`.

## Test plan

Summarized in Steps 7–8. Structural patterns: `OnPlanCreatedFillSummaryTest.java` (captor tests), `GetPlanHandlerTest.java` (handler tests). Regression coverage this plan must keep green: `GET /plans/{id}` response shape (id, meals with mealIndex/recipe.id/name/illustrationUrl), 404 behavior, null-recipe meals.

## Done criteria

- [ ] `./mvnw test` exits `BUILD SUCCESS`
- [ ] `grep -rn "OnRecipeChangedUpdatePlanSummary" src/` → no matches
- [ ] `grep -n "Repositories" src/main/java/nimnamfood/query/plan/projection/OnPlanCreatedFillSummary.java` → no matches
- [ ] `grep -rn "view_part_recipe_summary" src/main/java/ | wc -l` → ≥ 3 (two captors + `GetPlanHandler`)
- [ ] The migration file contains both the `create table view_part_recipe_summary` and the `update view_plans` transformation
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back if:

- The jsonb currently stored in `view_plans.meals` does not match the "Current state" shape (check `OnPlanCreatedFillSummary` and `PlansViewTestHelper` first — if they drifted, the migration's transformation is wrong).
- `${storage-bucket}` placeholder resolution fails when Flyway runs in tests — the placeholder configuration may have changed; do not hardcode a bucket name.
- The response-shape records (`PlanSummary`/`MealSummary`/`MealRecipeSummary`) would need to change to make anything work — the contract is frozen; report instead.
- You need to modify `view_recipes` or its projections — that's the recipe screens' private view; touching it means the design is being misapplied.

## Maintenance notes

- `view_part_recipe_summary` is now a **published contract of the recipe context**: plan queries (and future features) may join it. Changing its schema requires checking all readers — that's the deliberate trade against the deleted sync-back projection.
- Deferred follow-up: `view_plans` is now nearly isomorphic to the `plans`+`meals` write tables; dropping it entirely (reading write tables + summary part in `GetPlanHandler`) would delete `OnPlanCreatedFillSummary` too and remove the remaining read-your-writes race. Deferred because it crosses the project's "queries read views only" convention — a decision for the maintainer, not an executor.
- Recipe deletion (if ever implemented) must delete from `view_part_recipe_summary`; the read path already tolerates a missing row (renders a null recipe).
- Reviewer scrutiny points: the migration's jsonb transformation on real data, and that `GetPlanHandler` still 404s on unknown plan ids.
