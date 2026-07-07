# Plan 003: Run the real Flyway migrations in integration tests (retire the hand-maintained schema.sql)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/test/resources src/test/java/vtertre/infrastructure/persistence/jdbc/PostgresTestContainerBase.java`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: none
- **Category**: tests
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

Integration tests currently run against `src/test/resources/schema.sql`, a
136-line hand-maintained copy of the schema, with Flyway explicitly disabled.
The 13 real migrations in `src/main/resources/db/migration/postgresql/` are
therefore **never executed by any test** — a migration can be broken or drift
from schema.sql and the suite stays green; it only fails at deploy time.
After this plan, tests run the real migrations, so every new migration is
exercised by CI, and schema.sql shrinks to the single test-only table that has
no production counterpart.

## Current state

- `src/test/resources/application.properties` (entire file):

```properties
spring.sql.init.mode=always
spring.flyway.enabled=false
```

  Because this file shadows `src/main/resources/application.properties` in
  tests, the main file's Flyway settings (including
  `spring.flyway.placeholders.storage-bucket=nimnamfood-dev.appspot.com`) do
  NOT apply to tests.

- `src/test/resources/schema.sql` — `drop table if exists` + `create table`
  for every table and view-table, PLUS one test-only table `far` (lines 3-8)
  used by the framework test
  `src/test/java/vtertre/infrastructure/persistence/jdbc/JdbcRepositoryTest.java`
  via `FakeAggregateRootDbo` (`@Table("far")`). `far` exists in **no**
  migration — it must survive this change.

- `src/test/java/vtertre/infrastructure/persistence/jdbc/PostgresTestContainerBase.java`
  — base class for all DB tests: `@Tag("IO")`,
  `@Testcontainers(disabledWithoutDocker = true)`,
  `@DataJdbcTest(properties = "spring.test.database.replace=NONE")`, a static
  `PostgreSQLContainer` (`postgres:14`) with `.withReuse(true)`, and a
  `@DynamicPropertySource` wiring url/username/password.

- `@DataJdbcTest` is transactional per test (rollback after each test), so
  per-test data cleanup does not depend on schema.sql's drop/create behavior.
  However, **cross-context cleanup currently does**: every new Spring context
  re-runs schema.sql, dropping and recreating all tables. With Flyway instead,
  a reused container keeps its schema and any data committed outside test
  transactions. Test-helper writes happen inside test transactions (they are
  `@Autowired` beans called from test methods), so this should be safe — but
  watch for failures caused by leftover committed data.

- Migrations use one Flyway placeholder. `V7__create_recipe_search_view.sql:21`
  and `V8__create_recipes_view_and_rename_tags_view_part.sql:25` reference
  `${storage-bucket}` inside an illustration URL string. Tests must define this
  placeholder or Flyway fails.

- Flyway is on the classpath for tests (`flyway-core` +
  `flyway-database-postgresql` are main-scope deps in `pom.xml`), and
  `@DataJdbcTest` does not auto-configure Flyway... actually it does:
  `@DataJdbcTest` includes Flyway auto-configuration when
  `spring.flyway.enabled=true`. Migrations live under
  `classpath:db/migration/postgresql`, which Flyway finds because the default
  location `classpath:db/migration` is scanned recursively.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| One DB test class (needs Docker) | `./mvnw test -Dtest=JdbcRepositoryTest` | BUILD SUCCESS |
| Another DB test class | `./mvnw test -Dtest=GetPlansHandlerTest` | BUILD SUCCESS |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |
| Remove stale reused containers | `docker ps -a --filter label=org.testcontainers=true -q \| xargs docker rm -f` | ids printed (or nothing) |

## Scope

**In scope** (the only files you should modify):
- `src/test/resources/application.properties`
- `src/test/resources/schema.sql`

**Out of scope** (do NOT touch, even though they look related):
- Any file under `src/main/resources/db/migration/` — tests must run the
  migrations *as they are*; if one fails, that's a finding to report, not to fix here.
- `PostgresTestContainerBase.java` — container wiring is fine as-is. (Only
  exception: see STOP conditions if reuse causes migration state conflicts.)
- Test classes and test helpers — they should pass unchanged; a failure there
  is signal, not noise.

## Git workflow

- Branch: `advisor/003-run-flyway-migrations-in-tests`
- Commit style: French imperative, lowercase. Suggested: `exécuter les migrations flyway dans les tests`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 0: Clean slate

Reused Testcontainers from previous runs contain the schema.sql-created tables
with no Flyway history; migrations would collide with them
("relation already exists"). Remove them first:

```bash
docker ps -a --filter label=org.testcontainers=true -q | xargs docker rm -f
```

**Verify**: `docker ps -a --filter label=org.testcontainers=true -q` → empty output

### Step 1: Enable Flyway for tests

Replace the content of `src/test/resources/application.properties` with:

```properties
spring.sql.init.mode=always
spring.flyway.enabled=true
spring.flyway.placeholders.storage-bucket=nimnamfood-test.appspot.com
```

(`spring.sql.init.mode=always` stays — it now only applies the trimmed
schema.sql from Step 2. Spring Boot orders script-based initialization to run
after Flyway migrations.)

**Verify**: `./mvnw test -Dtest=GetPlansHandlerTest` → BUILD SUCCESS. If this
fails with "relation ... already exists" you skipped Step 0. If it fails
inside a `V*__*.sql` file, STOP (see STOP conditions).

### Step 2: Trim schema.sql to the test-only table

Replace the content of `src/test/resources/schema.sql` with only:

```sql
drop table if exists far;
create table far
(
    id   text primary key,
    name text not null
);
```

(Match the exact `far` definition currently at lines 3-8 of the existing file
— compare before replacing.)

**Verify**: `./mvnw test -Dtest=JdbcRepositoryTest` → BUILD SUCCESS (this is
the only consumer of `far`).

### Step 3: Full suite

Run everything twice — the second run exercises the container-reuse path
(Flyway sees applied migrations and skips them).

**Verify**: `./mvnw verify` → BUILD SUCCESS, then `./mvnw verify` again →
BUILD SUCCESS.

## Test plan

No new test files — the deliverable is that the ~15 existing Testcontainers
test classes now run against the migrated schema. The two consecutive
`./mvnw verify` runs in Step 3 are the acceptance test (fresh-migration path
and reused-container path).

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -c "create table" src/test/resources/schema.sql` returns 1
- [ ] `grep -n "spring.flyway.enabled=true" src/test/resources/application.properties` returns 1 match
- [ ] `./mvnw verify` exits 0, run twice in a row (Docker required)
- [ ] `git status` shows no modified files outside the in-scope list
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Any `V*__*.sql` migration itself fails when executed against a fresh
  Postgres 14 container — that is a real latent deploy bug; report which
  migration and the error, do not edit the migration.
- Tests fail because the migrated schema differs from what schema.sql declared
  (e.g. a column exists in schema.sql but in no migration) — that is the drift
  this plan exists to expose; report the exact difference.
- The second `./mvnw verify` run fails while the first passes — container
  reuse + Flyway interact badly; report rather than patching
  `PostgresTestContainerBase`.
- Committed leftover data from one test class breaks another (unique-constraint
  collisions across contexts) — report which helper commits outside the test
  transaction.

## Maintenance notes

- From now on, adding a migration is automatically covered by CI; schema.sql
  must NOT be extended again for production tables (only `far`-style test
  fixtures belong there).
- Developers with `testcontainers.reuse.enable=true` in
  `~/.testcontainers.properties` must remove their old reused container once
  (Step 0 command) after pulling this change — worth a line in the PR
  description.
- Plan 002 adds migration V14; if 002 lands after this plan, its migration is
  automatically exercised. If 002 landed first, verify V14 ran in tests.
