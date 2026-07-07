# Plan 008: Run the command bus on the IO (virtual-thread) executor

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/NimnamfoodConfiguration.java`
> If the file changed since this plan was written, compare the "Current
> state" excerpt against the live code before proceeding; on a mismatch,
> treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `ad32e0f`, 2026-07-07

## Why this matters

The command bus dispatches handlers on a fixed thread pool sized to `availableProcessors()` (the "Computation" pool), but command handlers in this codebase are almost entirely **blocking JDBC I/O** — `GeneratePlanCommandHandler` alone performs one candidate query per meal plus the aggregate insert. Under concurrent commands, a CPU-count pool of blocked threads queues requests while the machine idles. The query bus and event bus already run on the "Io" virtual-thread-per-task executor; commands should too. After the switch the Computation pool has no consumers and is removed.

## Current state

Single relevant file: `src/main/java/nimnamfood/NimnamfoodConfiguration.java`.

The two executors:

```java
@Bean
@Qualifier("Computation")
public ExecutorService fixedThreadPoolExecutorService() {
    return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("computation-pool-%d").build()
    );
}

@Bean
@Qualifier("Io")
public ExecutorService virtualThreadPerTaskExecutorService() {
    return Executors.newVirtualThreadPerTaskExecutor();
}
```

The command bus wiring (the only consumer of "Computation" — verified: `grep -rn '"Computation"' src/main/java` matches only these two spots in this file):

```java
@Bean
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public CommandBus commandBus(
        List<CommandMiddleware> middlewares,
        Set<CommandHandler<?, ?>> commandHandlers,
        @Qualifier("Computation") ExecutorService executorService) {
    return new CommandBusAsync(Sets.newLinkedHashSet(middlewares), commandHandlers, executorService);
}
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw test-compile` | `BUILD SUCCESS` |
| Full suite | `./mvnw test` | `BUILD SUCCESS` (Docker required — Testcontainers) |

## Scope

**In scope**:
- `src/main/java/nimnamfood/NimnamfoodConfiguration.java`
- `plans/README.md` (status update)

**Out of scope**:
- `CommandBusAsync` / `QueryBusAsync` / `EventBusAsync` framework classes.
- Adding new executors or tuning pool sizes.

## Git workflow

- Commit message style is **French, lowercase, imperative**. Suggested: `exécuter le bus de commandes sur l'executor IO`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Switch the qualifier and remove the dead pool

In `NimnamfoodConfiguration`:

1. In the `commandBus` bean method, change `@Qualifier("Computation")` to `@Qualifier("Io")`.
2. Delete the `fixedThreadPoolExecutorService()` bean method entirely, along with the now-unused imports (`ThreadFactoryBuilder`; check whether `Executors` is still used by the Io bean — it is, keep it).

**Verify**: `./mvnw test-compile` → `BUILD SUCCESS`; `grep -n "Computation" src/main/java/nimnamfood/NimnamfoodConfiguration.java` → no matches.

### Step 2: Full suite

**Verify**: `./mvnw test` → `BUILD SUCCESS`.

## Test plan

No new tests — the change is executor wiring; behavior is covered by every existing test that dispatches through the buses (Spring context tests fail if wiring breaks).

## Done criteria

- [ ] `./mvnw test` exits `BUILD SUCCESS`
- [ ] `grep -rn '"Computation"' src/main/java/` → no matches
- [ ] `grep -rn "newFixedThreadPool" src/main/java/nimnamfood/` → no matches
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back if:

- `grep -rn '"Computation"' src/` reveals a consumer other than the `commandBus` bean (drift — something else now depends on the fixed pool).
- Any test fails after the switch in a way that traces to thread pinning or `ThreadLocal` assumptions in command handlers (e.g. `ThreadLocalRandom` is fine, but report anything transaction-related).

## Maintenance notes

- If a genuinely CPU-bound command appears later (image processing, large in-memory computation), reintroduce a bounded computation pool for it rather than letting it saturate virtual threads.
- Virtual threads + JDBC: with Java 21, synchronized-block pinning inside the Postgres driver is the known caveat; at this project's scale it is a non-issue, but worth remembering if throughput problems appear.
