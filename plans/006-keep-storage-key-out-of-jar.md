# Plan 006: Load the Firebase service-account key from a file path and keep it out of the packaged jar

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/StorageConfiguration.java pom.xml src/main/resources/application.properties src/main/resources/application-production.properties README.md`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: MED (deployment contract changes — see Maintenance notes)
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

The Google Cloud service-account private key lives at
`src/main/resources/nimnamfood-storage-service-account-key-dev.json`. It is
gitignored (good — it is NOT in git history), but Maven packages everything
under `src/main/resources/` into the jar, so **every `mvn package` produces an
artifact containing the private key**. Anyone who obtains a built jar (CI
artifact, deploy platform, a shared build) can extract full credentials to the
storage bucket. Production uses the same mechanism with a different filename
(`storage-service-account-key-prod.json` on the classpath). This plan switches
to loading the key from a filesystem path outside the build tree and excludes
key files from packaging as a belt-and-braces measure.

**Never write the key's contents anywhere — not in the plan report, not in
logs, not in test fixtures. Reference it by path only.**

## Current state

- `src/main/java/nimnamfood/StorageConfiguration.java` (whole class body):

```java
@Bean
@Lazy
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
Storage storage(@Value("${nimnamfood.storage.private-key-filename}") String privateKeyResourceFilename,
                @Value("${nimnamfood.storage.project-id}") String projectId) throws IOException {
    final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(privateKeyResourceFilename);

    if (stream == null) {
        throw new RuntimeException("Failed to get service account key resource");
    }

    return StorageOptions
            .newBuilder()
            .setCredentials(GoogleCredentials.fromStream(stream))
            .setProjectId(projectId)
            .build()
            .getService();
}
```

  The bean is `@Lazy` — it is only created when storage is first used, which is
  why the whole test suite runs without any key file. Preserve `@Lazy`.

- `src/main/resources/application.properties:16`:
  `nimnamfood.storage.private-key-filename=nimnamfood-storage-service-account-key-dev.json`
- `src/main/resources/application-production.properties:16`:
  `nimnamfood.storage.private-key-filename=storage-service-account-key-prod.json`
- `.gitignore` last line ignores the dev key file.
- `pom.xml` has no `<resources>` customization — default resource copying.
- `README.md:13` instructs: "Add the Firebase service account key in
  `src/main/resources/nimnamfood-storage-service-account-key-dev.json`".

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw -q compile` | exit 0 |
| Package without running tests | `./mvnw -q package -DskipTests` | exit 0 |
| Prove no key in jar | `unzip -l target/*.jar \| grep -i "service-account"` | no matches (exit 1) |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |

## Scope

**In scope**:
- `src/main/java/nimnamfood/StorageConfiguration.java`
- `pom.xml` (resource exclusion only)
- `src/main/resources/application.properties`
- `src/main/resources/application-production.properties`
- `.gitignore` (add the new local key location)
- `README.md` (update setup instructions)

**Out of scope** (do NOT touch):
- `StorageAdapter.java` / `RecipeService.java` — consumers of the `Storage`
  bean, unaffected.
- The key files themselves — do not move/delete the operator's local
  `nimnamfood-storage-service-account-key-dev.json`; the operator migrates it
  themselves (README tells them how).
- CI workflow — it never had the key; nothing to change.

## Git workflow

- Branch: `advisor/006-keep-storage-key-out-of-jar`
- Commit style: French imperative, lowercase. Suggested: `charger la clé de stockage depuis un chemin externe`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Switch StorageConfiguration to a file path

Replace the property and the loading mechanism:

```java
Storage storage(@Value("${nimnamfood.storage.private-key-path}") String privateKeyPath,
                @Value("${nimnamfood.storage.project-id}") String projectId) throws IOException {
    try (final InputStream stream = new FileInputStream(privateKeyPath)) {
        return StorageOptions
                .newBuilder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
```

(`FileNotFoundException` from `FileInputStream` is a clear enough failure —
the custom RuntimeException wrapper can go. Note the original code never
closed the stream; use try-with-resources.)

**Verify**: `./mvnw -q compile` → exit 0

### Step 2: Update the properties

- `application.properties`: replace line 16 with
  `nimnamfood.storage.private-key-path=${STORAGE_KEY_PATH:secrets/nimnamfood-storage-service-account-key-dev.json}`
  (a `secrets/` directory at the repo root, overridable by env var).
- `application-production.properties`: replace line 16 with
  `nimnamfood.storage.private-key-path=${STORAGE_KEY_PATH}` — production must
  provide the path explicitly.
- `.gitignore`: add a line `secrets/`.

**Verify**: `grep -rn "private-key-filename" src/ --include='*.properties' --include='*.java'` → no matches

### Step 3: Belt-and-braces packaging exclusion

In `pom.xml`, inside `<build>`, add:

```xml
<resources>
    <resource>
        <directory>src/main/resources</directory>
        <excludes>
            <exclude>**/*service-account-key*.json</exclude>
        </excludes>
    </resource>
</resources>
```

This protects against the key file being re-added to resources out of habit.

**Verify**: `./mvnw -q package -DskipTests && unzip -l target/*.jar | grep -i "service-account"` → grep finds nothing (non-zero exit from grep is success here). Note the jar is a Boot fat jar — resources sit under `BOOT-INF/classes/`; `unzip -l` covers that.

### Step 4: README

Update the setup step to: create `secrets/` at the repo root, place the
service-account key there as
`nimnamfood-storage-service-account-key-dev.json` (or set `STORAGE_KEY_PATH`
to wherever it lives). Mention that production requires the `STORAGE_KEY_PATH`
env var pointing at a key file provisioned on the host.

**Verify**: `grep -n "secrets/" README.md` → at least 1 match

### Step 5: Full suite

The `Storage` bean is `@Lazy` and no test instantiates it, so tests pass
without any key file present.

**Verify**: `./mvnw verify` → BUILD SUCCESS (Docker required)

## Test plan

No new automated tests: the bean is a thin factory around the GCS SDK and is
`@Lazy`-instantiated; a unit test would only test `FileInputStream`. The
packaging exclusion is verified by the Step 3 jar inspection (add that command
to your report output). If the operator wants runtime proof, the Step 5 manual
path is: put a key at the default path, `./mvnw spring-boot:run`, upload an
illustration.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -rn "getResourceAsStream" src/main/java/nimnamfood/StorageConfiguration.java` → no matches
- [ ] `./mvnw -q package -DskipTests` exits 0 and `unzip -l target/*.jar | grep -ci "service-account"` outputs 0
- [ ] `grep -n "STORAGE_KEY_PATH" src/main/resources/application-production.properties` → 1 match
- [ ] `grep -n "secrets/" .gitignore` → 1 match
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Any test fails because it instantiates the `Storage` bean — the `@Lazy`
  assumption would be false; report which test.
- You find the key file is tracked by git after all
  (`git ls-files | grep service-account`) — that changes the severity
  (history rewrite + rotation needed); report immediately.
- The deployment platform's config (outside this repo) is discoverable and
  contradicts the env-var approach — don't guess; report.

## Maintenance notes

- **Deployment contract change**: the production host must provide the key
  file on disk and set `STORAGE_KEY_PATH` before this version deploys.
  Previously the key had to be smuggled into `src/main/resources/` at build
  time — whatever process did that must be retired. The operator owns this
  switch; flag it prominently in the PR.
- **Rotation recommended**: every jar built before this change contains the
  dev key. Rotate the dev service-account key in the Google Cloud console once
  this lands (and the prod key if prod jars ever left the deploy host).
- Future alternative: `GOOGLE_APPLICATION_CREDENTIALS` + `GoogleCredentials.getApplicationDefault()`
  is the GCP-idiomatic version of the same thing; the custom property was kept
  to minimize the diff.
