# Plan 009: Repo hygiene — remove dead entry point, fix CLAUDE.md, add a Docker-free unit-test profile, add a formatter check

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/Main.java CLAUDE.md pom.xml README.md .github/workflows/ci.yml`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

Four small frictions, none worth a plan alone, all worth one pass: (1) a dead
`Main.java` in the default package duplicates the real Spring Boot entry
point; (2) `CLAUDE.md` is a **broken symlink** to `docs/COMPANION.md`, which
was deleted in commit `730c81c` — any tooling that reads it fails; (3) you
cannot run the fast unit tests without Docker, although every DB test is
already tagged `@Tag("IO")`; (4) no formatter/style check exists, so style
drifts silently.

## Current state

- `src/main/java/Main.java` (entire file) — wraps
  `SpringApplication.run(NimnamfoodApiApplication.class, args)`. The real
  entry point is `src/main/java/nimnamfood/NimnamfoodApiApplication.java`
  (`@SpringBootApplication`). `pom.xml` does not pin a `start-class`, so the
  Boot plugin resolves the main class by scanning — **two** classes with
  `main` methods exist; removing `Main.java` removes the ambiguity.
- `CLAUDE.md` → symlink to `docs/COMPANION.md`; the `docs/` directory does not
  exist. Git tracks the symlink.
- DB-test tagging: `src/test/java/vtertre/infrastructure/persistence/jdbc/PostgresTestContainerBase.java:11`
  has `@Tag("IO")`; every Testcontainers test extends it. No Maven profile or
  surefire config uses the tag. Build tooling: Maven wrapper (`./mvnw`),
  Spring Boot parent 3.5.11, Java 21. CI: `.github/workflows/ci.yml` runs
  `mvn --batch-mode --no-transfer-progress verify` on push.
- `README.md` "Running tests" section only documents `./mvnw test` with Docker.
- No formatter config anywhere (no spotless/checkstyle in `pom.xml`, no
  `.editorconfig`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw -q compile` | exit 0 |
| Unit tests only (after Step 3, no Docker) | `./mvnw test -DexcludedGroups=IO` | BUILD SUCCESS |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |
| Formatter check (after Step 4) | `./mvnw spotless:check` | BUILD SUCCESS |

## Scope

**In scope**:
- `src/main/java/Main.java` (delete)
- `CLAUDE.md` (replace symlink with a real file)
- `pom.xml` (surefire `excludedGroups` support + spotless plugin)
- `README.md` (document the unit-test command)
- `.editorconfig` (create)

**Out of scope** (do NOT touch):
- `.github/workflows/ci.yml` — CI keeps running the full `verify` (it has
  Docker); do not split CI jobs in this plan.
- Any reformatting of existing source files beyond what Step 4's ratchet
  configuration requires (see Step 4 — the check must pass WITHOUT a bulk
  reformat commit).
- `.idea/`, `src/.DS_Store` — local noise, not in git; leave alone.

## Git workflow

- Branch: `advisor/009-repo-hygiene`
- Commit per step (4 small commits), French imperative, lowercase. E.g.:
  `supprimer le point d'entrée Main inutilisé`, `remplacer le lien symbolique CLAUDE.md`,
  `permettre l'exécution des tests unitaires sans docker`, `ajouter la vérification de formatage`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Delete Main.java

`git rm src/main/java/Main.java`

**Verify**: `./mvnw -q package -DskipTests` → exit 0, and
`unzip -p target/*.jar META-INF/MANIFEST.MF | grep Start-Class` →
`nimnamfood.NimnamfoodApiApplication`

### Step 2: Replace the CLAUDE.md symlink with a real file

`git rm CLAUDE.md`, then create a regular `CLAUDE.md` with this content
(adjust nothing you can't verify from the repo):

```markdown
# nimnamfood-api

Spring Boot 3 / Java 21 backend for Nimnamfood (daily meal assistant).

## Commands
- Build + all tests (Docker required): `./mvnw verify`
- Unit tests only (no Docker): `./mvnw test -DexcludedGroups=IO`
- Run locally: `docker compose up -d && ./mvnw spring-boot:run` (see README for the storage key setup)

## Architecture
- CQRS-lite. Commands (`nimnamfood/command/**`) are dispatched on an async
  `CommandBus`; handlers return `(result, domain events)`; events feed
  projections (`nimnamfood/query/**/projection`) that maintain `view_*` tables;
  queries (`nimnamfood/query/**`) read only those views.
- `vtertre/**` is the in-house framework layer (buses, DDD base classes,
  generic JDBC/memory repositories). App code lives under `nimnamfood/**`.
- Persistence: Spring Data JDBC + Flyway migrations in
  `src/main/resources/db/migration/postgresql/`. Every new `view_*` table needs
  a projection wired to its events.
- Repositories are reached through the static locator `nimnamfood.model.Repositories`.

## Conventions
- Commit messages: French, imperative, lowercase.
- Tests: JUnit 5 + AssertJ; DB integration tests extend
  `PostgresTestContainerBase` (tagged `IO`); unit tests use the in-memory
  repositories via `@ExtendWith(WithMemoryRepositories.class)`.
- Timestamps: `Instant` in Java, `timestamptz` in Postgres, written as
  `.atOffset(ZoneOffset.UTC)`; compared in tests with `isCloseTo(..., within(1, MICROS))`.
```

**Verify**: `test -f CLAUDE.md && test ! -L CLAUDE.md && echo ok` → `ok`

### Step 3: Unit-test run without Docker

In `pom.xml` `<properties>`, add nothing; surefire (managed by the Boot
parent) already supports `-DexcludedGroups`. Confirm it works as-is; only if
it does not, add to `<build><plugins>` an explicit
`maven-surefire-plugin` entry passing `<excludedGroups>${excludedGroups}</excludedGroups>`.
Then document in `README.md` under "Running tests":

```markdown
Unit tests only (no Docker needed):

```bash
./mvnw test -DexcludedGroups=IO
```
```

**Verify** (with Docker stopped or `-DexcludedGroups=IO` proving no container
starts): `./mvnw test -DexcludedGroups=IO` → BUILD SUCCESS and the run
completes without pulling/starting a Postgres container (no Testcontainers
log lines).

### Step 4: Formatter as a ratchet, not a big bang

Add the Spotless Maven plugin to `pom.xml` configured so the existing code
passes without a mass reformat: use `ratchetFrom` = `origin/main` (only files
changed since the base are checked) with the `googleJavaFormat` (AOSP variant,
4-space indent, matching the codebase) or `palantirJavaFormat`. Also create a
minimal `.editorconfig` (4-space indent for `*.java`, UTF-8, final newline).
Do NOT wire spotless into CI or into the `verify` lifecycle in this plan —
check-only, invoked manually.

**Verify**: `./mvnw spotless:check` → BUILD SUCCESS on the branch (only your
own changed files are checked, and they are formatted).

## Test plan

No new tests. Gates: Step 1's manifest check, Step 3's Docker-free run,
`./mvnw verify` (full, Docker) at the end → BUILD SUCCESS.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `test -e src/main/java/Main.java` → non-existent
- [ ] `test -f CLAUDE.md && test ! -L CLAUDE.md` → true
- [ ] `./mvnw test -DexcludedGroups=IO` exits 0 without Docker
- [ ] `./mvnw spotless:check` exits 0
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Something references `Main` (grep `"Main"` in `pom.xml`, `Procfile`,
  `system.properties`, deploy configs) — the "dead code" premise would be
  false. Check before deleting.
- `-DexcludedGroups=IO` still starts a Postgres container — some DB test
  doesn't extend the tagged base class; report which instead of tagging it
  yourself.
- Spotless cannot be configured to pass without touching more than ~5 existing
  files — report; a bulk-reformat decision belongs to the operator.

## Maintenance notes

- Follow-up candidates deliberately excluded: wiring `spotless:check` into CI,
  and splitting CI into a fast unit job + a Docker job. Both are trivial once
  this lands; do them when the suite gets slow.
- `CLAUDE.md` content goes stale like any doc — whoever adds a new
  architectural layer (or changes the test commands) should update it in the
  same PR.
