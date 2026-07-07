# Plan 005: Move `RecipeTagFilterRequirement` from infrastructure into the domain

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/infrastructure/repository/RecipeTagFilterRequirement.java src/main/java/nimnamfood/model/recipe src/main/java/nimnamfood/command/plan src/main/java/nimnamfood/infrastructure/repository/jdbc/recipe src/main/java/nimnamfood/infrastructure/repository/memory`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW (pure move + import updates, no behavior change)
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `ad32e0f`, 2026-07-07

## Why this matters

`RecipeTagFilterRequirement` — a value object describing tag constraints ("must have these tags, must not have those, must have one of each of these groups") — lives in `nimnamfood.infrastructure.repository`, yet the **domain-layer** interface `nimnamfood.model.recipe.RecipeRepository` uses it in its signature. That makes the domain depend on infrastructure, inverting the dependency rule this codebase otherwise follows (model defines interfaces, infrastructure implements them). The type is a domain concept and belongs in `nimnamfood.model.recipe`. This unblocks plan 006 (a planning-side port that reuses the type).

## Current state

The type to move (`src/main/java/nimnamfood/infrastructure/repository/RecipeTagFilterRequirement.java`, entire file):

```java
package nimnamfood.infrastructure.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record RecipeTagFilterRequirement(Set<UUID> requiredTagsIds, Set<UUID> excludedTagIds,
                                         List<Set<UUID>> oneOfTagsIdsCombinations) {
}
```

Every file importing it (verified with `grep -rln "RecipeTagFilterRequirement" src/`):

| File | Role |
|------|------|
| `src/main/java/nimnamfood/model/recipe/RecipeRepository.java` | domain interface — the offending dependency (`import nimnamfood.infrastructure.repository.RecipeTagFilterRequirement;`) |
| `src/main/java/nimnamfood/command/plan/GeneratePlanCommandHandler.java` | builds requirements from the command |
| `src/main/java/nimnamfood/infrastructure/repository/jdbc/recipe/RecipeJdbcRepository.java` | SQL implementation |
| `src/main/java/nimnamfood/infrastructure/repository/memory/RecipeMemoryRepository.java` | in-memory implementation |

There should be **no other** references; re-run the grep yourself before starting.

Convention note: domain value objects and events already live in `nimnamfood.model.<aggregate>` (e.g. `nimnamfood.model.recipe.RecipeIngredient`, `RecipeChanged`) — the move follows the existing pattern.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw test-compile` | `BUILD SUCCESS` |
| Fast tests (no Docker) | `./mvnw test -Dtest=GeneratePlanCommandHandlerTest` | `BUILD SUCCESS` |
| Full suite | `./mvnw test` | `BUILD SUCCESS` (Docker required) |

## Scope

**In scope**:
- `src/main/java/nimnamfood/infrastructure/repository/RecipeTagFilterRequirement.java` (delete)
- `src/main/java/nimnamfood/model/recipe/RecipeTagFilterRequirement.java` (create)
- Import lines only in the four files listed above
- `plans/README.md` (status update)

**Out of scope**:
- Renaming the record or its accessors (`requiredTagsIds`, `excludedTagIds`, `oneOfTagsIdsCombinations`) — tempting cleanup, but keep this a pure move.
- Any logic change in `GeneratePlanCommandHandler` — that is plan 006.
- Deleting the `nimnamfood.infrastructure.repository` package folder — it still contains `jdbc/` and `memory/`.

## Git workflow

- Commit message style is **French, lowercase, imperative**. Suggested: `déplacer RecipeTagFilterRequirement dans le domaine`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Move the file

Create `src/main/java/nimnamfood/model/recipe/RecipeTagFilterRequirement.java` with the exact same record body, package changed to `nimnamfood.model.recipe`. Delete `src/main/java/nimnamfood/infrastructure/repository/RecipeTagFilterRequirement.java`.

### Step 2: Fix the imports

- `RecipeRepository.java`: remove `import nimnamfood.infrastructure.repository.RecipeTagFilterRequirement;` — the type is now in the same package, no import needed.
- `GeneratePlanCommandHandler.java`, `RecipeJdbcRepository.java`, `RecipeMemoryRepository.java`: change the import to `nimnamfood.model.recipe.RecipeTagFilterRequirement`.

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 3: Confirm no stragglers

**Verify**: `grep -rn "infrastructure.repository.RecipeTagFilterRequirement" src/` → no matches.

### Step 4: Run the suite

**Verify**: `./mvnw test` → `BUILD SUCCESS`.

## Test plan

No new tests — this is a package move with zero behavior change; the existing `GeneratePlanCommandHandlerTest` (8 tests) and repository tests fully cover the type's usage.

## Done criteria

- [ ] `./mvnw test` exits `BUILD SUCCESS`
- [ ] `grep -rn "infrastructure.repository.RecipeTagFilterRequirement" src/` → no matches
- [ ] `test -f src/main/java/nimnamfood/model/recipe/RecipeTagFilterRequirement.java` → exists; old path gone
- [ ] `grep -c "import nimnamfood" src/main/java/nimnamfood/model/recipe/RecipeRepository.java` → `0` (domain interface imports nothing from outside the model)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back if:

- The grep in "Current state" reveals additional referencing files not listed here (drift) — list them and stop.
- Compilation surfaces a name clash in `nimnamfood.model.recipe` (a type with that name already added there).

## Maintenance notes

- Plan 006 builds directly on this: the planning port's signature uses `nimnamfood.model.recipe.RecipeTagFilterRequirement`.
- Reviewer check: the diff should contain only a file move and import-line changes — any other hunk is scope creep.
