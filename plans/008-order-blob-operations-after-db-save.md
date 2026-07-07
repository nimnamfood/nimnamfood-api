# Plan 008: Perform illustration blob operations after the recipe DB write, inside the handler transaction

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/command/recipe src/test/java/nimnamfood/command/recipe`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: MED
- **Depends on**: none (001 recommended first so failures are visible in logs)
- **Category**: bug
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

Recipe create/update handlers touch two systems: Google Cloud Storage blobs
(illustration images) and the Postgres `recipes` tables. Today the blob
operations run **first**:

- Create: the pending blob is promoted to `live/` *before* the recipe row is
  inserted. If the insert fails (duplicate key, DB down), a live blob is
  orphaned and its pending copy is gone.
- Update with a new illustration: `replaceIllustration` activates the new blob
  AND **deletes the old live blob** *before* the DB update. If the update then
  fails, the still-referenced old illustration is gone — the recipe's image is
  permanently broken.

`CommandHandler.execute` is `@Transactional`
(`src/main/java/vtertre/command/CommandHandler.java:11`), so moving the blob
operations *after* the repository write but *inside* the method gives: a blob
failure rolls the DB write back (consistent), and a DB failure never touches
blobs (consistent). The only remaining hole is a failure of the commit itself
after blob ops — much rarer than the current exposure. Full two-phase
consistency (sagas/outbox) was considered and rejected as disproportionate.

## Current state

- `src/main/java/nimnamfood/command/recipe/CreateRecipeCommandHandler.java:31-43`:

```java
if (illustrationId != null) {
    this.recipeService.activateIllustration(illustrationId);   // blob op FIRST
}

final Tuple<Recipe, RecipeCreated> tuple = Recipe.factory().create(
        command.name, illustrationId, command.portionsCount,
        recipeIngredients, command.instructions, tagIds
);
Repositories.recipes().add(tuple._1);                          // DB write SECOND
```

- `src/main/java/nimnamfood/command/recipe/UpdateRecipeCommandHandler.java:41-55`:

```java
if (newIllustrationId != null && !newIllustrationId.equals(currentIllustrationId)) {
    this.activateNewIllustration(currentIllustrationId, newIllustrationId);   // blob ops FIRST
} else if (newIllustrationId == null && currentIllustrationId != null) {
    this.recipeService.deleteIllustration(currentIllustrationId);
}

final Tuple<Recipe, RecipeChanged> tuple = currentRecipe.get().updated(...);
Repositories.recipes().update(tuple._1);                                       // DB write SECOND
```

  `activateNewIllustration` calls `recipeService.activateIllustration(new)`
  when there is no current illustration, else
  `recipeService.replaceIllustration(current, new)` (which activates the new
  blob then deletes the old — see `RecipeService.java:43-46`).

- `src/main/java/nimnamfood/service/RecipeService.java:48-61` —
  `activateIllustration` throws `MissingBlobException` when the pending blob
  doesn't exist; `deleteIllustration` is best-effort delete.

- Existing tests:
  `src/test/java/nimnamfood/command/recipe/CreateRecipeCommandHandlerTest.java`
  and `UpdateRecipeCommandHandlerTest.java` — JUnit 5,
  `@ExtendWith(WithMemoryRepositories.class)` (in-memory repositories reached
  via the static `Repositories` locator), `RecipeService recipeService =
  Mockito.mock();`, handlers constructed by hand. They already verify *which*
  blob methods are called (`Mockito.verify(recipeService, times(1))
  .replaceIllustration(...)` etc.) but not *when*.

- **Important test-environment fact**: the in-memory repositories are not
  transactional — rollback semantics exist only with the JDBC repositories
  under Spring transaction management. Unit tests therefore assert *ordering*
  (Mockito `InOrder`) and *propagation* (exception bubbles up), NOT rollback.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw -q compile` | exit 0 |
| Handler tests (no Docker) | `./mvnw test -Dtest='CreateRecipeCommandHandlerTest,UpdateRecipeCommandHandlerTest'` | BUILD SUCCESS |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |

## Scope

**In scope**:
- `src/main/java/nimnamfood/command/recipe/CreateRecipeCommandHandler.java`
- `src/main/java/nimnamfood/command/recipe/UpdateRecipeCommandHandler.java`
- `src/test/java/nimnamfood/command/recipe/CreateRecipeCommandHandlerTest.java`
- `src/test/java/nimnamfood/command/recipe/UpdateRecipeCommandHandlerTest.java`

**Out of scope** (do NOT touch):
- `RecipeService.java` — `replaceIllustration`'s internal activate-then-delete
  order is correct; only the *callers'* ordering moves.
- `ImportIllustrationCommandHandler` — single-system operation, nothing to
  order.
- Transaction configuration, outbox tables, retry/cleanup jobs — explicitly
  rejected for now (see Why this matters).

## Git workflow

- Branch: `advisor/008-order-blob-operations-after-db-save`
- Commit style: French imperative, lowercase. Suggested: `déplacer les opérations de blobs après l'écriture en base`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Reorder CreateRecipeCommandHandler

Move the `activateIllustration` call to after `Repositories.recipes().add(tuple._1);`
(keep the null guard). Resulting order: build aggregate → `add` → activate
blob → return tuple.

**Verify**: `./mvnw -q compile` → exit 0

### Step 2: Reorder UpdateRecipeCommandHandler

Move the whole illustration `if/else if` block to after
`Repositories.recipes().update(tuple._1);`. The variables it needs
(`newIllustrationId`, `currentIllustrationId`) are computed earlier and remain
in scope. Resulting order: load current → build updated aggregate → `update`
→ blob operations → return tuple.

**Verify**: `./mvnw -q compile` → exit 0

### Step 3: Ordering tests

Add to each handler test (following the existing Mockito style):

- Create: `activatesTheIllustrationAfterSavingTheRecipe` — use
  `Mockito.inOrder` on a spy-wrapped repository? The repositories come from
  the static locator and are not mocks — instead assert ordering indirectly:
  stub `recipeService.activateIllustration` with
  `Mockito.doAnswer(invocation -> { assertThat(Repositories.recipes().get(recipeId)).isPresent(); return null; })`
  — i.e. when the blob op runs, the recipe must already be saved. (For create,
  capture the id via the command result or query the repo by name.)
- Update (same technique): when `replaceIllustration` runs, the repo's recipe
  must already carry the NEW illustration id; when `deleteIllustration` runs
  (removal case), the repo's recipe must already have a null illustration id.
- Failure propagation: stub `activateIllustration` to throw
  `MissingBlobException`; assert the handler propagates it
  (`assertThatExceptionOfType`). Do NOT assert repository rollback — memory
  repositories don't roll back (see Current state).

**Verify**: `./mvnw test -Dtest='CreateRecipeCommandHandlerTest,UpdateRecipeCommandHandlerTest'` → BUILD SUCCESS, 3+ new tests

### Step 4: Full suite

**Verify**: `./mvnw verify` → BUILD SUCCESS (Docker required)

## Test plan

Covered in Step 3. Pattern: existing `UpdateRecipeCommandHandlerTest` (memory
repositories via `@ExtendWith(WithMemoryRepositories.class)`, mocked
`RecipeService`, `Mockito.verify`).

## Done criteria

Machine-checkable. ALL must hold:

- [ ] In both handlers, the `Repositories.recipes().add(...)`/`.update(...)` call
      textually precedes every `recipeService.*Illustration*` call
      (inspect with `grep -n "recipeService\.\|Repositories.recipes()" src/main/java/nimnamfood/command/recipe/*CommandHandler.java`)
- [ ] `./mvnw test -Dtest='CreateRecipeCommandHandlerTest,UpdateRecipeCommandHandlerTest'` exits 0 with new ordering tests
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Any existing test asserts the OLD ordering (blob before save) as desired
  behavior in a way that suggests it was deliberate (e.g. a comment explaining
  it) — the reordering premise would need operator confirmation.
- You find the `@Transactional` on `CommandHandler.execute` is not effective
  (e.g. handlers invoked in a way that bypasses the Spring proxy — check how
  `InvokeCommandHandlerMiddleware` gets its handler instances) makes the
  rollback rationale false. The reordering is still an improvement, but report
  the discovery — it changes the "Why this matters" guarantees.

## Maintenance notes

- Remaining known gap (accepted): a commit failure after the blob ops, or a
  process crash mid-handler, can still orphan a pending/live blob. If this
  ever matters, the next step is a periodic reconciliation job listing
  `pending/` blobs older than N days — not a saga.
- Reviewer should scrutinize: in the update flow, the *event* (`RecipeChanged`)
  is returned and published after the handler regardless; nothing in the event
  path depends on blob-op timing.
- Interacts with plan 002/001: projection failures after these handlers are
  logged (001) and the plan views stay consistent (002).
