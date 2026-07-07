# Plan 003: Make projection failures observable and plan projections synchronous

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/vtertre/infrastructure/bus/event src/main/java/nimnamfood/model/plan/PlanCreated.java src/test/java/vtertre/infrastructure/bus/event`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: none (but land after 001 so `PlanCreated` covers both plan projections)
- **Category**: bug
- **Planned at**: commit `ad32e0f`, 2026-07-07

## Why this matters

The event bus (`EventBusAsync`) runs projections asynchronously and **discards the resulting `CompletableFuture`**, so any exception thrown by a projection is silently dropped — not even logged. If the projection that fills the `view_plans` read model fails, the plan aggregate exists but `GET /plans/{id}` returns 404 forever, with zero trace of why. There is also a read-your-writes race: `POST /plans/generate` returns the new plan's id before the projection has committed, so an immediate `GET` can transiently miss.

Three changes: (1) log dropped projection exceptions, (2) make "event has no captor" a non-error instead of a swallowed `NoSuchElementException`, (3) annotate `PlanCreated` with the existing-but-unused `@Synced` annotation so plan projections run synchronously within the command dispatch, eliminating the race and making failures visible to the caller.

## Current state

Relevant files:

- `src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java` — the bus; contains both defects.
- `src/main/java/vtertre/ddd/event/Synced.java` — runtime annotation, checked by the bus, currently applied to no event.
- `src/main/java/nimnamfood/model/plan/PlanCreated.java` — the event to annotate.
- `src/test/java/vtertre/infrastructure/bus/event/EventBusAsyncTest.java` — existing bus tests; extend them.
- `src/main/java/vtertre/infrastructure/bus/event/EventPublisherMiddleware.java` — publishes events after the command handler completes (read-only reference).

Defect 1 — swallowed exceptions (`EventBusAsync.java`):

```java
@Override
public <T extends DomainEvent> void publish(List<T> events) {
    events.forEach(this::execute);          // returned future is discarded
}

private <T extends DomainEvent> CompletableFuture<Boolean> execute(T event) {
    final ExecutorService executor = event.getClass().getAnnotation(Synced.class) != null ? this.directExecutorService : this.executorService;
    return CompletableFuture.supplyAsync(() -> firstMiddlewareChainLink.apply(event), executor);
}
```

Note: even for `@Synced` events, `supplyAsync` on the **direct executor** captures exceptions in the future rather than throwing to the caller — so today `@Synced` would still swallow errors. That must change (Step 3).

Defect 2 — no-captor events throw (`EventBusAsync.java`, inner class `CaptorInvocation`):

```java
return this.captors
        .stream()
        .filter(captor -> captor.eventType().equals(event.getClass()))
        .map(captor -> { ... ((EventCaptor<T>) captor).execute(event); return true; })
        .reduce((a, b) -> a && b)
        .orElseThrow();      // NoSuchElementException when no captor matches
```

The existing test `anEventCanHaveNoCaptors` in `EventBusAsyncTest` passes **only because** the exception is captured in the discarded future — it asserts `publish` doesn't throw, which is true even though the chain fails internally. Once exceptions become observable, this defect becomes real.

The event to annotate (`PlanCreated.java`):

```java
public record PlanCreated(UUID id, Instant createdAt, ImmutableSet<Meal> meals) implements DomainEvent {
}
```

Context on ordering: `EventPublisherMiddleware` calls `eventBus.publish(...)` inside `thenApply` after the command handler has already persisted the aggregate. With `@Synced`, captors run on that same thread; a captor exception then propagates into the command's future and surfaces as an HTTP 500 — while the aggregate is already saved. That trade-off is intended: a loud failure over a silently missing read model.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw test-compile` | `BUILD SUCCESS` |
| Bus tests | `./mvnw test -Dtest=EventBusAsyncTest` | `BUILD SUCCESS` |
| Full suite | `./mvnw test` | `BUILD SUCCESS` (Docker required — Testcontainers) |

## Scope

**In scope**:
- `src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java`
- `src/main/java/nimnamfood/model/plan/PlanCreated.java`
- `src/test/java/vtertre/infrastructure/bus/event/EventBusAsyncTest.java`
- `plans/README.md` (status update)

**Out of scope**:
- `EventPublisherMiddleware.java` — publish-after-handler ordering stays as is.
- Other domain events (`RecipeCreated`, `RecipeChanged`, ...) — do not annotate them; recipe projections staying async is deliberate.
- Any outbox/persistence mechanism for events — explicitly deferred (see Maintenance notes).
- `NimnamfoodConfiguration.java` — executor wiring is plan 008.

## Git workflow

- Commit message style is **French, lowercase, imperative**. Suggested: `journaliser les erreurs de projection et synchroniser PlanCreated`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Tolerate events without captors

In `EventBusAsync.CaptorInvocation.apply`, replace the terminal operation so zero matching captors is a logged no-op instead of `NoSuchElementException`:

```java
@Override
public <T extends DomainEvent> boolean apply(T event) {
    final var matchingCaptors = this.captors
            .stream()
            .filter(captor -> captor.eventType().equals(event.getClass()))
            .toList();

    if (matchingCaptors.isEmpty()) {
        LOGGER.debug("No captor registered for event {}", event.getClass().getSimpleName());
        return true;
    }

    matchingCaptors.forEach(captor -> {
        LOGGER.debug("Applying captor {}", captor.getClass());
        ((EventCaptor<T>) captor).execute(event);
    });
    return true;
}
```

(`LOGGER` already exists on the outer class; inner classes are static — make `CaptorInvocation` use the outer `LOGGER` field directly, which is legal since it is `static`.)

**Verify**: `./mvnw test -Dtest=EventBusAsyncTest` → `BUILD SUCCESS` (the `anEventCanHaveNoCaptors` test still passes, now for the right reason).

### Step 2: Log dropped async failures

In `EventBusAsync.execute`, attach an exception handler to the future for the **async** path:

```java
private <T extends DomainEvent> CompletableFuture<Boolean> execute(T event) {
    if (event.getClass().getAnnotation(Synced.class) != null) {
        // Step 3 replaces this branch
    }
    return CompletableFuture.supplyAsync(() -> firstMiddlewareChainLink.apply(event), this.executorService)
            .exceptionally(throwable -> {
                LOGGER.error("Captor chain failed for event {}", event, throwable);
                return false;
            });
}
```

### Step 3: Make `@Synced` events fail loudly

For `@Synced` events, run the chain **directly on the calling thread** so exceptions propagate to the publisher (and, via `EventPublisherMiddleware`, into the command's future):

```java
private <T extends DomainEvent> CompletableFuture<Boolean> execute(T event) {
    if (event.getClass().getAnnotation(Synced.class) != null) {
        return CompletableFuture.completedFuture(firstMiddlewareChainLink.apply(event));
    }
    return CompletableFuture.supplyAsync(() -> firstMiddlewareChainLink.apply(event), this.executorService)
            .exceptionally(throwable -> {
                LOGGER.error("Captor chain failed for event {}", event, throwable);
                return false;
            });
}
```

The `directExecutorService` field becomes unused — remove it and its import (`MoreExecutors`).

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`; `grep -n "directExecutorService" src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java` → no matches.

### Step 4: Annotate `PlanCreated`

```java
import vtertre.ddd.event.Synced;

@Synced
public record PlanCreated(UUID id, Instant createdAt, ImmutableSet<Meal> meals) implements DomainEvent {
}
```

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`

### Step 5: Extend `EventBusAsyncTest`

Add to the existing test class (reuse its `FakeEvent` / `FakeEventCaptor` inner-class style):

1. `syncedEventCaptorExceptionPropagatesToPublisher` — define `@Synced private static class SyncedFakeEvent implements DomainEvent {}` and a captor whose `execute` throws `RuntimeException`; assert `bus.publish(List.of(new SyncedFakeEvent()))` throws (use `assertThatExceptionOfType(RuntimeException.class)` — note `CompletableFuture` is not involved on this path, the exception is raw).
2. `syncedEventRunsOnCallingThread` — captor records `Thread.currentThread()`; publish from the test thread with a real single-thread executor passed to the bus constructor; assert the captor saw the test thread.
3. `asyncCaptorExceptionDoesNotPropagate` — non-annotated event, throwing captor, direct executor service (existing `MoreExecutors.newDirectExecutorService()` pattern in the test): `publish` must not throw.

**Verify**: `./mvnw test -Dtest=EventBusAsyncTest` → `BUILD SUCCESS`, all old + 3 new tests pass.

### Step 6: Full suite

**Verify**: `./mvnw test` → `BUILD SUCCESS`. Pay attention to `GeneratePlanCommandHandlerTest` and the command-bus tests — they should be unaffected (they call handlers directly, not through the bus), but confirm.

## Test plan

Covered by Step 5. Pattern: `src/test/java/vtertre/infrastructure/bus/event/EventBusAsyncTest.java` (fake events/captors, direct executor service).

## Done criteria

- [ ] `./mvnw test` exits `BUILD SUCCESS`, including 3 new `EventBusAsyncTest` cases
- [ ] `grep -rn "orElseThrow" src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java` → no matches
- [ ] `grep -n "@Synced" src/main/java/nimnamfood/model/plan/PlanCreated.java` → one match
- [ ] `grep -rn "@Synced" src/main/java/nimnamfood/model/ | grep -v plan` → no matches (no other event annotated)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back if:

- `EventBusAsync` no longer matches the excerpts (drift).
- After Step 4, any full-suite test fails in a way that traces to `PlanCreated` captors now running synchronously (e.g. a transaction/threading assumption in `OnPlanCreatedFillSummary`) — report the failing test and stack trace rather than reworking the captor.
- You find another mechanism already handling captor failures (a middleware that catches and logs) — the audit found none, but if one exists the design here needs rethinking.

## Maintenance notes

- **Behavior change**: `POST /plans/generate` now returns 500 if a plan projection fails, even though the plan aggregate was persisted. That is intentional (loud > silently missing read model), but the reviewer should sign off on it explicitly.
- Events remain in-memory only: a crash between aggregate save and captor execution still loses the projection. The durable fix is an outbox table — deliberately out of scope; reconsider if this bites in production.
- `EventCaptor.execute` is `@Transactional` (interface-level) — each captor still gets its own transaction when invoked through its Spring proxy; synchronous execution does not join it with the command's persistence work.
- If future projections become slow, `@Synced` on `PlanCreated` adds their latency to the generate endpoint — revisit then.
