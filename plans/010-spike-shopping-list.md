# Plan 010: Spike — shopping list generated from a plan (design + prototype, not full build)

> **Executor instructions**: This is a DESIGN SPIKE, not a build plan. The
> deliverable is a design document plus a throwaway-quality prototype of the
> core query, proving the data model supports the feature and surfacing the
> open product questions. Follow the steps; if anything in the "STOP
> conditions" section occurs, stop and report. When done, update the status
> row for this plan in `plans/README.md` — unless a reviewer dispatched you
> and told you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/model src/main/resources/db/migration`
> If the domain model changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: M (coarse — spikes are bounded by time, not scope: stop after ~a day)
- **Risk**: LOW (no production code changes)
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

The app calls itself a "Daily Meal Assistant" (`pom.xml` description). It can
already generate a meal plan from tag filters, and its data model carries
everything a shopping list needs: recipes have ingredients with quantities and
units, plans reference recipes. Aggregating a plan's recipe ingredients into a
shopping list is the single most on-mission feature the existing schema
supports without new tables. This spike answers the design questions so a
later build plan can be written honestly — it does NOT ship the feature.

## Current state (grounding evidence)

- Domain model:
  - `src/main/java/nimnamfood/model/recipe/RecipeIngredient.java` — fields:
    `UUID ingredientId`, `float quantity`, `IngredientUnit unit`.
  - `src/main/java/nimnamfood/model/ingredient/IngredientUnit.java` — enum:
    `GRAM, MILLILITER, TABLESPOON, TEASPOON, PIECE, PINCH, HANDFUL, SLICE, LEAF`.
  - `src/main/java/nimnamfood/model/recipe/Recipe.java` — also has
    `int portionsCount` (a plan meal serves some number of people — scaling
    question below).
  - `src/main/java/nimnamfood/model/plan/Plan.java` / `Meal.java` — a plan is a
    set of `Meal(mealIndex, recipeId)`; `recipeId` may be NULL (empty slot —
    migration `V11__allow_null_recipe_id_in_meals.sql`).
- Relevant tables (from `V1__initial_entities.sql`, `V10__add_plan_tables_and_views.sql`):
  `plans`, `meals(plan_id, meal_index, recipe_id)`, `recipes`,
  `recipe_ingredients(recipe_id, ingredient_id, quantity, unit)`,
  `ingredients(id, name, unit)`.
- Read-side conventions this feature must follow:
  - Queries extend `vtertre.query.QueryHandlerJdbc` and read from tables/views
    — exemplar: `src/main/java/nimnamfood/query/plan/GetPlanHandler.java`.
  - Endpoints live in `src/main/java/nimnamfood/web/*Resource.java` and return
    `Future<...>` via `queryBus.dispatch(...)` — exemplar:
    `PlanResource.java` (`GET /plans/{stringUuid}`).
  - New view tables need projections for every event that changes their inputs
    — that's the expensive path (see trade-off below).
- Also relevant: a NOTE for the designer — `GeneratePlanCommandHandler.java:35`
  picks each meal's recipe independently at random, so a plan can contain the
  same recipe twice; the shopping list must handle duplicate recipe ids
  (quantities count twice).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| DB test for the prototype (needs Docker) | `./mvnw test -Dtest=ShoppingListSpikeTest` | BUILD SUCCESS |
| Full suite unaffected | `./mvnw verify` | BUILD SUCCESS |

## Scope

**In scope** (all new, all clearly marked as spike output):
- `plans/010-spike-shopping-list-FINDINGS.md` (create — the design document, the real deliverable)
- `src/test/java/nimnamfood/query/plan/ShoppingListSpikeTest.java` (create — prototype query as a Testcontainers test; delete or keep per operator decision, note it in FINDINGS)

**Out of scope** (do NOT touch):
- ALL production code (`src/main/java/**`) — no endpoint, no handler, no
  migration lands in this spike.
- The duplicate-recipe generation behavior — note it, don't fix it.

## Git workflow

- Branch: `advisor/010-spike-shopping-list`
- One commit at the end: `explorer la génération de liste de courses`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Prototype the aggregation query

Write `ShoppingListSpikeTest` extending `PostgresTestContainerBase`
(pattern: `GetPlansHandlerTest`). Seed via SQL/`NamedParameterJdbcTemplate`:
two recipes sharing one ingredient with the same unit and one ingredient
each with different units; a plan whose meals reference recipe 1 twice and
recipe 2 once, plus one NULL-recipe meal. Then prototype the read query,
starting from:

```sql
select i.id, i.name, ri.unit, sum(ri.quantity) as quantity
from meals m
join recipe_ingredients ri on ri.recipe_id = m.recipe_id
join ingredients i on i.id = ri.ingredient_id
where m.plan_id = :planId
group by i.id, i.name, ri.unit
order by i.name
```

Assert: the shared same-unit ingredient sums across meals (recipe used twice
counts twice), different units for one ingredient produce separate rows, the
NULL meal contributes nothing.

**Verify**: `./mvnw test -Dtest=ShoppingListSpikeTest` → BUILD SUCCESS

### Step 2: Write the design document

Create `plans/010-spike-shopping-list-FINDINGS.md` answering, with a
recommendation each (recommendations may be revised by what Step 1 taught you):

1. **Read path**: live aggregation query on the write-model tables (as
   prototyped) vs a `view_shopping_list` projection. Note that the CQRS
   convention here is views — but a projection would need to react to
   `PlanCreated`, `RecipeChanged`, `IngredientChanged`, and future plan-edit
   events; weigh that maintenance cost against one JOIN at read time on a
   single-user dataset. State a recommendation.
2. **API shape**: propose `GET /plans/{id}/shopping-list` response JSON
   (ingredient id, name, quantity, unit; grouping; ordering), consistent with
   existing summary records (see `query/plan/model/*.java` naming).
3. **Unit merging**: quantities only merge within the same `IngredientUnit`.
   Is `2 TABLESPOON + 30 GRAM` of the same ingredient two lines (recommended:
   yes, no conversion table) or converted?
4. **Portion scaling**: recipes have `portionsCount`; plans have no "people
   count". Ship v1 unscaled (recipe-as-written) or add scaling input? List
   what scaling would require.
5. **Empty slots & duplicates**: NULL-recipe meals skipped; duplicate recipes
   double quantities — confirm from the Step 1 test.
6. **Build estimate**: files to create for the real feature (query, handler,
   resource, tests), S/M/L.

### Step 3: Report

Summarize the recommendation and open product questions (3-4 max) in your
final message AND at the top of the FINDINGS file.

## Test plan

The spike test IS the prototype; it must pass. No other suite changes:
`./mvnw verify` → BUILD SUCCESS.

## Done criteria

- [ ] `plans/010-spike-shopping-list-FINDINGS.md` exists, answers all 6 questions, each with a stated recommendation
- [ ] `./mvnw test -Dtest=ShoppingListSpikeTest` exits 0 (Docker required)
- [ ] `git status` confirms zero changes under `src/main/`
- [ ] `./mvnw verify` exits 0
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The schema lacks something the aggregation needs (e.g. `recipe_ingredients`
  rows missing for some recipes) — that's a data-model finding; report it.
- You are tempted to implement the endpoint "since the query already works" —
  out of scope; the operator decides after reading the findings.
- The spike exceeds roughly a day of effort — write down what's unresolved and
  stop; an over-long spike is a failed spike.

## Maintenance notes

- The follow-up build plan (if the operator green-lights) should be written
  against the FINDINGS file and follow plan-002/004 conventions.
- If plan editing (swap/regenerate meals) is built later, a projection-based
  shopping list would need those new events too — one more argument the
  designer should weigh in question 1.
