# Plan 001: Make the event bus log projection failures and tolerate events without captors

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java src/test/java/vtertre/infrastructure/bus/event/EventBusAsyncTest.java`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

This app is CQRS-lite: command handlers write aggregates, then domain events are
published on an async event bus, and "captor" classes (projections) update
read-model view tables. `EventBusAsync.publish()` creates a `CompletableFuture`
per event and **discards it**. Any exception thrown by a projection — SQL
constraint violation, JSON serialization error, missing row — vanishes: no log,
no metric, nothing. The read views silently drift from the write model. A real
production bug (newly generated plans never appearing in `GET /plans`, fixed by
plan 002) survived unnoticed partly because of this. Bonus bug in the same
class: publishing an event that has **zero registered captors** throws
`NoSuchElementException` from `reduce(...).orElseThrow()` — which is then also
swallowed. A third weakness: captors for the same event run sequentially in one
future, so the first failing captor silently prevents all later captors from
running.

After this plan: every captor failure is logged at ERROR with the event and
captor class; one failing captor no longer prevents the others; events with no
captors are a no-op instead of a hidden exception.

## Current state

- `src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java` — the async event bus. The whole file is ~80 lines; the relevant parts:

```java
// EventBusAsync.java:31-39
@Override
public <T extends DomainEvent> void publish(List<T> events) {
    events.forEach(this::execute);
}

private <T extends DomainEvent> CompletableFuture<Boolean> execute(T event) {
    final ExecutorService executor = event.getClass().getAnnotation(Synced.class) != null ? this.directExecutorService : this.executorService;
    return CompletableFuture.supplyAsync(() -> firstMiddlewareChainLink.apply(event), executor);
}
```

```java
// EventBusAsync.java:65-77 (inner class CaptorInvocation)
@Override
public <T extends DomainEvent> boolean apply(T event) {
    return this.captors
            .stream()
            .filter(captor -> captor.eventType().equals(event.getClass()))
            .map(captor -> {
                LOGGER.debug("Applying captor {}", captor.getClass());
                ((EventCaptor<T>) captor).execute(event);
                return true;
            })
            .reduce((a, b) -> a && b)
            .orElseThrow();          // <-- throws NoSuchElementException when no captor matches
}
```

- The class already has a logger: `private final static Logger LOGGER = LoggerFactory.getLogger(EventBusAsync.class);` (line 19, slf4j).
- `src/main/java/vtertre/ddd/event/EventCaptor.java` — the captor interface; `execute` is `@Transactional`, so each captor invocation is its own transaction. A try/catch around one captor does not affect another captor's transaction.
- `src/test/java/vtertre/infrastructure/bus/event/EventBusAsyncTest.java` — existing unit test. It uses `MoreExecutors.newDirectExecutorService()` (Guava) so everything runs synchronously on the test thread. Note: the existing test `anEventCanHaveNoCaptors` (line 23-27) currently passes **only because the exception is swallowed inside the discarded future** — after your change it must pass for the right reason.
- Repo conventions: no Mockito needed here — the test file defines hand-rolled `FakeEvent`, `FakeEventCaptor`, `FakeMiddleware` inner classes. Follow that style. AssertJ is used for assertions.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw -q compile` | exit 0 |
| This test class only | `./mvnw test -Dtest=EventBusAsyncTest` | BUILD SUCCESS, 0 failures |
| Full suite (needs Docker running) | `./mvnw verify` | BUILD SUCCESS |

## Scope

**In scope** (the only files you should modify):
- `src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java`
- `src/test/java/vtertre/infrastructure/bus/event/EventBusAsyncTest.java`

**Out of scope** (do NOT touch, even though they look related):
- `EventPublisherMiddleware.java`, `CommandBusAsync.java`, `QueryBusAsync.java` — command/query buses have different error contracts (their futures ARE returned to callers).
- Any captor/projection class under `src/main/java/nimnamfood/query/` — projection bugs are handled by plan 002.
- Do not add a dead-letter queue, retry mechanism, or metrics library — logging only. Bigger machinery was considered and rejected as disproportionate for this app.

## Git workflow

- Branch: `advisor/001-log-event-captor-failures`
- Commit message style is French imperative, lowercase, no prefix — e.g. existing commit `db15561 simplifier les bus`. Suggested: `journaliser les échecs de captors d'événements`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Isolate and log captor failures in `CaptorInvocation.apply`

Rewrite the method body so that each matching captor runs in its own try/catch;
on exception, log at ERROR with the captor class, the event class, and the
exception as the final logging argument (so the stack trace prints); continue
with the next captor. When no captor matches, do nothing and return `true`
(the existing test `anEventCanHaveNoCaptors` documents that this is legal).
Target shape:

```java
@Override
public <T extends DomainEvent> boolean apply(T event) {
    this.captors.stream()
            .filter(captor -> captor.eventType().equals(event.getClass()))
            .forEach(captor -> {
                LOGGER.debug("Applying captor {}", captor.getClass());
                try {
                    ((EventCaptor<T>) captor).execute(event);
                } catch (Exception exception) {
                    LOGGER.error("Captor {} failed for event {}", captor.getClass(), event, exception);
                }
            });
    return true;
}
```

**Verify**: `./mvnw -q compile` → exit 0

### Step 2: Log failures escaping the future itself

`execute(event)` still returns a discarded future; middleware (upstream of the
captors) could also throw. Attach a completion handler instead of discarding
silently, e.g. in `publish`/`execute`:

```java
private <T extends DomainEvent> CompletableFuture<Boolean> execute(T event) {
    final ExecutorService executor = event.getClass().getAnnotation(Synced.class) != null ? this.directExecutorService : this.executorService;
    return CompletableFuture.supplyAsync(() -> firstMiddlewareChainLink.apply(event), executor)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    LOGGER.error("Event bus failed to process event {}", event, exception);
                }
            });
}
```

**Verify**: `./mvnw test -Dtest=EventBusAsyncTest` → BUILD SUCCESS (existing 3 tests still pass)

### Step 3: Add regression tests

In `EventBusAsyncTest`, following the existing fake-class style:

1. `aFailingCaptorDoesNotPreventOtherCaptors` — two `FakeEventCaptor`s where the
   first one's `execute` throws `RuntimeException`; publish; assert the second
   captor was called and no exception propagated to the caller. (Add a
   `boolean shouldThrow` field or a small `ThrowingCaptor` inner class.)
2. `aFailingMiddlewareDoesNotPropagate` — a middleware whose `intercept` throws;
   publish; assert no exception propagates (this exercises the Step 2 path).

Note the test uses a direct executor, so "no exception propagated" is a
meaningful synchronous assertion.

**Verify**: `./mvnw test -Dtest=EventBusAsyncTest` → BUILD SUCCESS, 5 tests, 0 failures

## Test plan

Covered by Step 3. Pattern to follow: the existing inner fakes in
`EventBusAsyncTest.java` (no mocking framework). Final check: `./mvnw verify`
with Docker running → all tests in the repo pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./mvnw test -Dtest=EventBusAsyncTest` exits 0 with 5+ tests
- [ ] `grep -n "orElseThrow" src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java` returns no matches
- [ ] `grep -n "LOGGER.error" src/main/java/vtertre/infrastructure/bus/event/EventBusAsync.java` returns at least 2 matches
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `EventBusAsync.java` no longer matches the "Current state" excerpts (drifted).
- You find code that *depends on* the returned `CompletableFuture<Boolean>` of
  `execute` or on `publish` throwing (e.g. a caller keeping the future) — the
  fire-and-forget assumption would be false.
- Any existing test outside this file starts failing after the change — that
  would mean something relied on the swallowed-exception behavior.

## Maintenance notes

- Plan 002 fixes a projection bug this logging would have surfaced; land this
  first so any future projection failure is visible in logs.
- Reviewer should scrutinize: the catch is `Exception`, not `Throwable` —
  intentional; an `Error` should still surface via the Step 2 handler.
- Deferred deliberately: dead-letter/retry for failed projections and a
  view-rebuild command. Revisit only if failures actually show up in logs.
