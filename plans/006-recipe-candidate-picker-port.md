# Plan 006: Introduce a `RecipeCandidatePicker` port and deduplicate candidate queries in plan generation

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/command/plan src/main/java/nimnamfood/model/plan src/main/java/nimnamfood/infrastructure/repository src/test/java/nimnamfood/command/plan`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition. **Exception**: plan 005 legitimately
> moved `RecipeTagFilterRequirement` to `nimnamfood.model.recipe` — that change
> is expected, not drift.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW
- **Depends on**: plans/005-move-recipe-tag-filter-requirement-to-domain.md
- **Category**: tech-debt (+ perf)
- **Planned at**: commit `ad32e0f`, 2026-07-07

## Why this matters

Two problems in `GeneratePlanCommandHandler`:

1. **Boundary**: the planning code reaches directly into the recipe context's repository (`Repositories.recipes().findIdsByTagFilterRequirement(...)`). "Give me recipe candidates matching tag constraints" is a capability the planning context *needs*; it should be expressed as a port owned by the planning domain (`nimnamfood.model.plan`), with an adapter in infrastructure. This makes the plan↔recipe seam explicit and testable.
2. **Performance**: the handler runs one full candidate query **per meal**. A 7-meal plan whose meals share the same (usually global-only) filters executes 7 identical queries, each materializing every matching recipe id. Memoizing candidates per distinct requirement within one command execution removes the redundancy without changing selection semantics (each meal still picks independently at random from the same candidate set).

## Current state

Prerequisite state (after plan 005): `RecipeTagFilterRequirement` lives at `src/main/java/nimnamfood/model/recipe/RecipeTagFilterRequirement.java`:

```java
package nimnamfood.model.recipe;

public record RecipeTagFilterRequirement(Set<UUID> requiredTagsIds, Set<UUID> excludedTagIds,
                                         List<Set<UUID>> oneOfTagsIdsCombinations) {
}
```

If it is still in `nimnamfood.infrastructure.repository`, plan 005 has not run — STOP.

The handler (`src/main/java/nimnamfood/command/plan/GeneratePlanCommandHandler.java`), current core:

```java
@Component
public class GeneratePlanCommandHandler implements CommandHandler<GeneratePlanCommand, UUID> {
    @Override
    public Tuple<UUID, List<DomainEvent>> execute(GeneratePlanCommand command) {
        final var globalRequired = toUuids(command.globalTagFilters.has);
        final var globalExcluded = toUuids(command.globalTagFilters.doesNotHave);
        final var globalOneOf = command.globalTagFilters.hasOneOf.stream()
                .map(GeneratePlanCommandHandler::toUuids)
                .toList();

        final var meals = command.meals.stream().map(mealCommand -> {
            final var requirement = combineFilters(globalRequired, globalExcluded, globalOneOf, mealCommand.tagFilters);
            final var candidateIds = new ArrayList<>(Repositories.recipes().findIdsByTagFilterRequirement(requirement));

            if (candidateIds.isEmpty()) {
                return new Meal(mealCommand.mealIndex, null);
            }

            final var selectedRecipeId = candidateIds.get(ThreadLocalRandom.current().nextInt(candidateIds.size()));
            return new Meal(mealCommand.mealIndex, selectedRecipeId);
        }).collect(Collectors.toSet());

        final var tuple = Plan.factory().create(meals);
        Repositories.plans().add(tuple._1);

        return tuple.map((plan, event) -> Tuple.of(plan.getId(), List.of(event)));
    }
    // combineFilters(...) and toUuids(...) helpers below — keep them unchanged
}
```

Wiring context you need to know:

- Command handlers are Spring `@Component`s collected into the command bus (`NimnamfoodConfiguration.commandBus` takes `Set<CommandHandler<?, ?>>`). Adding a constructor dependency works as long as the dependency is itself a bean.
- `Repositories` (`src/main/java/nimnamfood/model/Repositories.java`) is a static service locator; tests swap implementations via JUnit extensions `WithMemoryRepositories` / `WithJdbcRepositories`. The adapter below deliberately goes through `Repositories.recipes()` so both test setups keep working unchanged.
- The test (`src/test/java/nimnamfood/command/plan/GeneratePlanCommandHandlerTest.java`, 8 tests) constructs the handler with `new GeneratePlanCommandHandler()` under `@ExtendWith(WithMemoryRepositories.class)`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw test-compile` | `BUILD SUCCESS` |
| Handler tests (no Docker) | `./mvnw test -Dtest=GeneratePlanCommandHandlerTest` | `BUILD SUCCESS`, 8+ tests |
| Full suite | `./mvnw test` | `BUILD SUCCESS` (Docker required) |

## Scope

**In scope**:
- `src/main/java/nimnamfood/model/plan/RecipeCandidatePicker.java` (create)
- `src/main/java/nimnamfood/infrastructure/repository/RecipeRepositoryCandidatePicker.java` (create)
- `src/main/java/nimnamfood/command/plan/GeneratePlanCommandHandler.java` (modify)
- `src/test/java/nimnamfood/command/plan/GeneratePlanCommandHandlerTest.java` (modify constructor calls; add memoization test)
- `plans/README.md` (status update)

**Out of scope**:
- `RecipeRepository` / `RecipeJdbcRepository.findIdsByTagFilterRequirement` — the SQL stays where it is; the port delegates to it.
- Moving random selection into SQL (`ORDER BY random()`) — would defeat memoization; deliberately not done.
- Removing the static `Repositories` locator — much larger change, deferred.
- `combineFilters` / `toUuids` helpers — unchanged.

## Git workflow

- Commit message style is **French, lowercase, imperative**. Suggested: `introduire le port RecipeCandidatePicker et mémoïser les candidats`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Create the port (planning domain)

`src/main/java/nimnamfood/model/plan/RecipeCandidatePicker.java`:

```java
package nimnamfood.model.plan;

import nimnamfood.model.recipe.RecipeTagFilterRequirement;

import java.util.Set;
import java.util.UUID;

public interface RecipeCandidatePicker {
    Set<UUID> candidateRecipeIds(RecipeTagFilterRequirement requirement);
}
```

### Step 2: Create the adapter (infrastructure)

`src/main/java/nimnamfood/infrastructure/repository/RecipeRepositoryCandidatePicker.java`:

```java
package nimnamfood.infrastructure.repository;

import nimnamfood.model.Repositories;
import nimnamfood.model.plan.RecipeCandidatePicker;
import nimnamfood.model.recipe.RecipeTagFilterRequirement;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class RecipeRepositoryCandidatePicker implements RecipeCandidatePicker {
    @Override
    public Set<UUID> candidateRecipeIds(RecipeTagFilterRequirement requirement) {
        return Repositories.recipes().findIdsByTagFilterRequirement(requirement);
    }
}
```

Going through `Repositories.recipes()` (rather than injecting a repository bean) is deliberate: it works identically under `WithMemoryRepositories` in tests and `JdbcRepositories` at runtime.

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 3: Refactor the handler

In `GeneratePlanCommandHandler`:

1. Add a `private final RecipeCandidatePicker recipeCandidatePicker;` field with a constructor (annotate the constructor `@Autowired` to match repo style — see `GetPlanHandler` for the convention).
2. Replace the per-meal candidate lookup with a per-execution memo. Target shape of `execute`:

```java
@Override
public Tuple<UUID, List<DomainEvent>> execute(GeneratePlanCommand command) {
    final var globalRequired = toUuids(command.globalTagFilters.has);
    final var globalExcluded = toUuids(command.globalTagFilters.doesNotHave);
    final var globalOneOf = command.globalTagFilters.hasOneOf.stream()
            .map(GeneratePlanCommandHandler::toUuids)
            .toList();

    final var candidatesByRequirement = new HashMap<RecipeTagFilterRequirement, List<UUID>>();

    final var meals = command.meals.stream().map(mealCommand -> {
        final var requirement = combineFilters(globalRequired, globalExcluded, globalOneOf, mealCommand.tagFilters);
        final var candidateIds = candidatesByRequirement.computeIfAbsent(
                requirement,
                r -> List.copyOf(this.recipeCandidatePicker.candidateRecipeIds(r)));

        if (candidateIds.isEmpty()) {
            return new Meal(mealCommand.mealIndex, null);
        }

        final var selectedRecipeId = candidateIds.get(ThreadLocalRandom.current().nextInt(candidateIds.size()));
        return new Meal(mealCommand.mealIndex, selectedRecipeId);
    }).collect(Collectors.toSet());

    final var tuple = Plan.factory().create(meals);
    Repositories.plans().add(tuple._1);

    return tuple.map((plan, event) -> Tuple.of(plan.getId(), List.of(event)));
}
```

Memoization correctness note: `RecipeTagFilterRequirement` is a record over `Set`/`List` fields, so `equals`/`hashCode` are structural — meals with identical combined filters share one lookup. The `List<Set<UUID>>` field makes equality order-sensitive for `hasOneOf` groups; identical filters built by the same `combineFilters` path produce the same order, so at worst a differently-ordered equivalent requirement causes an extra query, never a wrong result.

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 4: Update the tests

In `GeneratePlanCommandHandlerTest`, replace every `new GeneratePlanCommandHandler()` with `new GeneratePlanCommandHandler(new RecipeRepositoryCandidatePicker())` (the adapter resolves through the memory repositories installed by the extension). All 8 existing tests must pass unmodified otherwise.

Add one new test proving memoization:

```java
@Test
void queriesCandidatesOncePerDistinctFilterCombination() {
    AtomicInteger calls = new AtomicInteger();
    RecipeCandidatePicker countingPicker = requirement -> {
        calls.incrementAndGet();
        return Repositories.recipes().findIdsByTagFilterRequirement(requirement);
    };
    GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler(countingPicker);

    Recipe recipe = Recipe.factory().create("recette", 1, Collections.emptySet(), "")._1;
    Repositories.recipes().add(recipe);

    GeneratePlanCommand command = new GeneratePlanCommand();
    command.meals = List.of(mealConfig(0), mealConfig(1), mealConfig(2)); // same (empty) filters

    handler.execute(command);

    assertThat(calls.get()).isEqualTo(1);
}
```

**Verify**: `./mvnw test -Dtest=GeneratePlanCommandHandlerTest` → `BUILD SUCCESS`, 9 tests pass.

### Step 5: Full suite

**Verify**: `./mvnw test` → `BUILD SUCCESS`. This also proves Spring wires the handler: the context-loading tests will fail if the adapter bean is missing.

## Test plan

- All 8 existing `GeneratePlanCommandHandlerTest` cases pass with only the constructor change.
- New case: `queriesCandidatesOncePerDistinctFilterCombination` (Step 4).
- Pattern file: `GeneratePlanCommandHandlerTest.java` itself.

## Done criteria

- [ ] `./mvnw test` exits `BUILD SUCCESS`, 9 tests in `GeneratePlanCommandHandlerTest`
- [ ] `grep -n "Repositories.recipes()" src/main/java/nimnamfood/command/plan/GeneratePlanCommandHandler.java` → no matches (only `Repositories.plans()` remains)
- [ ] `test -f src/main/java/nimnamfood/model/plan/RecipeCandidatePicker.java` and the adapter file exist
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back if:

- `RecipeTagFilterRequirement` is still in `nimnamfood.infrastructure.repository` (plan 005 not executed).
- The handler no longer matches the excerpt (drift beyond plan 005's expected move).
- Spring context tests fail with an unsatisfied dependency on `RecipeCandidatePicker` after Step 5 — report rather than adding config; the `@Component` on the adapter should suffice.

## Maintenance notes

- The port returns the full candidate set by design (enables memoization + independent random picks). If recipe counts ever grow large enough that materializing ids hurts, change the port to a batch signature (`pick(requirement, count)`) and push sampling into SQL — revisit the memoization then.
- If per-meal "no repeats within a plan" logic is ever added, it belongs in the handler on top of the memoized candidate lists — the port should stay a pure query.
- Reviewer check: selection semantics are unchanged (same candidate set per requirement, independent uniform pick per meal).
