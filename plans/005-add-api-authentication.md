# Plan 005: Require a bearer token on all API endpoints

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/web src/main/resources/application.properties src/main/resources/application-local.properties src/main/resources/application-production.properties`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: plans/004-http-layer-test-baseline.md
- **Category**: security
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

The API is deployed (production profile, CI, CORS config for a browser client)
and has **no authentication of any kind**: `pom.xml` has no security
dependency, and no controller or filter checks identity. Anyone who discovers
the URL can create/overwrite recipes, ingredients, tags, and plans, and upload
files into the Google Cloud Storage bucket (`POST /illustrations`). This plan
adds the smallest credible protection for a single-user app: a static bearer
token, supplied via environment variable, checked by a servlet filter on every
request except the health endpoint.

**Design decision (made during planning, revisit with the operator if it seems
wrong):** a hand-rolled `OncePerRequestFilter` with a constant-time token
comparison, WITHOUT adding Spring Security. Rationale: Spring Security's
auto-configuration would lock down everything by default, break all existing
`@WebMvcTest` tests in non-obvious ways, and bring CSRF/session machinery this
token-based single-user API doesn't need. Alternative rejected for now:
Firebase Auth ID-token verification (the `firebase-admin` dependency could do
it) — better multi-user story, but requires frontend work and user management;
noted as a future migration path in Maintenance notes.

## Current state

- `pom.xml` — no `spring-boot-starter-security`, no other auth dependency. Do
  not add any.
- Controllers: `src/main/java/nimnamfood/web/*Resource.java` — 10
  `@RestController` classes, no identity checks anywhere.
  `HealthResource.java` serves `GET /` and is used as the liveness check —
  it must stay unauthenticated:

```java
@RestController
public class HealthResource {
    @GetMapping("/")
    ...
}
```

- Configuration files:
  - `src/main/resources/application.properties` — common defaults; contains
    e.g. `cors.allowed-origins=*` (line 13) and `nimnamfood.storage.*` keys.
    Property convention: kebab-case, `nimnamfood.` prefix for app-specific keys.
  - `src/main/resources/application-local.properties` — local dev values,
    plain-text local-only DB password (fine).
  - `src/main/resources/application-production.properties` — everything
    sensitive comes from environment placeholders, e.g.
    `spring.datasource.password=${DB_PASSWORD}` and
    `cors.allowed-origins=${CORS_ALLOWED_ORIGINS}`. Follow this pattern.
- CORS: `src/main/java/nimnamfood/web/configuration/CorsConfiguration.java`
  reads `cors.allowed-origins` and calls `.allowedHeaders("*")` — wildcard
  headers means the `Authorization` header is already allowed; no CORS change
  is strictly required, but Step 4 tightens the dev default.
- HTTP tests from plan 004: `src/test/java/nimnamfood/web/*ResourceTest.java`
  — `@WebMvcTest` slices. **`@WebMvcTest` auto-registers plain servlet
  `Filter` beans only if they are picked up by the slice's component filters;
  an `@Component` filter IS included.** These tests will start failing with
  401 once the filter exists — Step 5 handles them.
- Exception body convention (match it for the 401 body):
  `NimnamfoodExceptionHandler` returns `{"error": "<CODE>"}` maps, e.g.
  `Collections.singletonMap("error", "DUPLICATE_IDENTIFIER")`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw -q compile` | exit 0 |
| Filter unit/slice tests | `./mvnw test -Dtest='ApiTokenFilterTest,*ResourceTest'` | BUILD SUCCESS |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |
| Manual check (app running) | `curl -s -o /dev/null -w '%{http_code}' localhost:8080/recipes` | `401` |
| Manual check with token | `curl -s -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer <token>' localhost:8080/recipes` | `200` |

## Scope

**In scope**:
- `src/main/java/nimnamfood/web/configuration/ApiTokenFilter.java` (create)
- `src/main/resources/application.properties` (add `nimnamfood.api.token` default + tighten `cors.allowed-origins`)
- `src/main/resources/application-local.properties` (add local token value)
- `src/main/resources/application-production.properties` (add `nimnamfood.api.token=${API_TOKEN}`)
- `src/test/java/nimnamfood/web/configuration/ApiTokenFilterTest.java` (create)
- `src/test/java/nimnamfood/web/*ResourceTest.java` (adapt to the filter)
- `README.md` (document the env var)

**Out of scope** (do NOT touch):
- `pom.xml` — no new dependencies.
- `NimnamfoodExceptionHandler.java` — the filter writes its own 401 response
  (filters run before the dispatcher; `@ControllerAdvice` can't help there).
- Firebase/GCS code — bucket-level ACLs are a separate concern.
- The Testcontainers test suite — repository/projection tests don't go through
  HTTP and must not need tokens.

## Git workflow

- Branch: `advisor/005-add-api-authentication`
- Commit style: French imperative, lowercase. Suggested: `exiger un jeton d'api sur les endpoints`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: The filter

Create `src/main/java/nimnamfood/web/configuration/ApiTokenFilter.java`
(same package as `CorsConfiguration`):

```java
@Component
public class ApiTokenFilter extends OncePerRequestFilter {
    private final byte[] expectedToken;

    public ApiTokenFilter(@Value("${nimnamfood.api.token}") String expectedToken) {
        this.expectedToken = expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // health check stays open; CORS preflights carry no Authorization header
        return "/".equals(request.getRequestURI())
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && MessageDigest.isEqual(this.expectedToken,
                        header.substring("Bearer ".length()).getBytes(StandardCharsets.UTF_8))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
    }
}
```

Notes that matter: `MessageDigest.isEqual` for constant-time comparison; the
OPTIONS exemption keeps CORS preflight working (browsers don't send
`Authorization` on preflight); jakarta imports (`jakarta.servlet.*`).

**Verify**: `./mvnw -q compile` → exit 0

### Step 2: Configuration

- `application.properties`: add `nimnamfood.api.token=${API_TOKEN:}` — empty
  default so a missing env var fails closed (empty expected token never equals
  a supplied one... **but it WOULD equal a request with an empty bearer
  token**; therefore also reject when the configured token is blank: in the
  constructor, `if (expectedToken.isBlank()) LOGGER.warn(...)` and in the check
  require `this.expectedToken.length > 0`).
- `application-local.properties`: `nimnamfood.api.token=local-dev-token`
  (non-secret, local only, mirrors the committed local DB password convention).
- `application-production.properties`: `nimnamfood.api.token=${API_TOKEN}`
  (hard placeholder — production fails to boot without it, which is desired).

**Verify**: `./mvnw -q compile` → exit 0

### Step 3: Filter tests

Create `src/test/java/nimnamfood/web/configuration/ApiTokenFilterTest.java` as
a plain unit test (construct the filter with a known token,
`MockHttpServletRequest`/`MockHttpServletResponse`, a `MockFilterChain`):
valid token → chain called; missing header → 401 + JSON body; wrong token →
401; `GET /` without token → chain called; `OPTIONS /recipes` without token →
chain called; blank configured token + blank bearer → 401.

**Verify**: `./mvnw test -Dtest=ApiTokenFilterTest` → BUILD SUCCESS

### Step 4: Tighten the dev CORS default

In `application.properties`, change `cors.allowed-origins=*` to
`cors.allowed-origins=http://localhost:4200`. Check first what the frontend
dev origin actually is: if you can't determine it, keep `*` and note it —
do not break the operator's local setup over this secondary hardening.

**Verify**: `./mvnw -q compile` → exit 0

### Step 5: Adapt the plan-004 HTTP tests

The `@Component` filter now applies inside `@WebMvcTest` slices (the slice
needs the `nimnamfood.api.token` property and the header). Choose ONE
consistent approach for all `*ResourceTest` classes:

Preferred: add to each test class
`@TestPropertySource(properties = "nimnamfood.api.token=test-token")` and a
shared request post-processor / default header
(`header(HttpHeaders.AUTHORIZATION, "Bearer test-token")`) on every request.
This keeps the filter in the tested path — one of the resource tests should
additionally assert that a request WITHOUT the header gets 401, proving the
filter is actually engaged in the slice.

**Verify**: `./mvnw test -Dtest='*ResourceTest'` → BUILD SUCCESS

### Step 6: README + full suite

Add to `README.md` (Running locally section): the API requires
`Authorization: Bearer <token>`; locally the token is `local-dev-token`; in
production set the `API_TOKEN` env var (generate with `openssl rand -hex 32`).

**Verify**: `./mvnw verify` → BUILD SUCCESS (Docker required)

## Test plan

Step 3 (filter unit tests, 6 cases) + Step 5 (every resource test passes with
the header; at least one 401 test proves enforcement). Pattern: plan 004's
test classes.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./mvnw test -Dtest='ApiTokenFilterTest,*ResourceTest'` exits 0
- [ ] `grep -n "MessageDigest.isEqual" src/main/java/nimnamfood/web/configuration/ApiTokenFilter.java` returns 1 match
- [ ] `grep -n "API_TOKEN" src/main/resources/application-production.properties` returns 1 match
- [ ] `grep -rn "nimnamfood.api.token" src/main/resources/*.properties | wc -l` returns 3
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] No secret values committed: the only token literal in the repo is `local-dev-token` (and `test-token` in tests)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Plan 004's tests don't exist yet (this plan depends on them as its safety
  net) — execute 004 first or report.
- The filter breaks the Testcontainers suite — those tests must not touch HTTP;
  if one does, report which.
- You find an existing auth mechanism (drift) — do not layer a second one.
- Anything requires adding a dependency to `pom.xml`.

## Maintenance notes

- **The operator must set `API_TOKEN` in the production environment and update
  the frontend to send the header BEFORE deploying this change** — deploying
  API-first breaks the client. Coordinate the rollout; this is the main risk.
- Migration path if multi-user ever matters: swap the filter internals for
  Firebase ID-token verification (`FirebaseAuth.getInstance().verifyIdToken`)
  — the filter boundary and tests stay, only the check changes.
- Reviewer should scrutinize: the blank-token edge case (empty configured token
  must never authenticate) and the OPTIONS exemption (needed for CORS, but
  confirm no state-changing handler is mapped to OPTIONS).
