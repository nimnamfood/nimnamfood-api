# Migration plan: JUnit 5 → Spock 2

Goal: convert every test class under `src/test/java` to a Spock specification written in Groovy under `src/test/groovy`. Test **helpers stay in Java**. Behavior must not change: this is a 1:1 mechanical migration, no test logic changes, no merging of tests into `where:` data tables, no renaming of what is asserted.

## Current state (verified inventory)

- Maven project, Spring Boot parent 3.5.11, Java 21. Single `pom.xml`. CI runs `mvn verify` (`.github/workflows/ci.yml`), no test-tag filtering anywhere — the `@Tag("IO")` on `PostgresTestContainerBase` is unused by the build.
- 44 test classes (149 `@Test` methods), 20 helper classes (view test helpers annotated `@Service`, `*Inspector` records, `Fake*` classes, `ObjectMapperFactory`, plus 3 pieces of JUnit-specific infrastructure that must be replaced — see below).
- Libraries in use: JUnit Jupiter, AssertJ, Mockito, Testcontainers (`junit-jupiter` integration), Spring Boot test slice `@DataJdbcTest`.
- Commit messages in this repo are in French, imperative, lowercase (e.g. `migrer les LocalDateTime pertinents vers Instant`). Follow that style.

Three test-infrastructure classes are JUnit-specific and get replaced (step 2):

1. `vtertre/infrastructure/persistence/jdbc/PostgresTestContainerBase.java` — abstract base, singleton PostgreSQL Testcontainer started in a static block, `@DataJdbcTest` + `@DynamicPropertySource`. 24 test classes extend it.
2. `nimnamfood/infrastructure/repository/memory/WithMemoryRepositories.java` — JUnit `BeforeEachCallback/AfterEachCallback` that does `Repositories.initialize(new MemoryRepositories())` / `initialize(null)`. Used via `@ExtendWith` by 7 test classes.
3. `nimnamfood/infrastructure/repository/jdbc/WithJdbcRepositories.java` — same idea but builds a `JdbcRepositories` from Spring beans. Used by exactly 1 test class (`OnPlanCreatedFillSummaryTest`).

## Step 0 — Baseline

Run `mvn verify` (Docker must be running). Record the total test count from the surefire summary. **This number must be identical after the migration.** Expect roughly 149.

## Step 0.5 — Prep refactor: controllable clock (removes all `isCloseTo` assertions)

Done **before** the Spock migration, on the existing JUnit tests, so the migrated specs never contain temporal-tolerance workarounds.

Background: `Instant.now()` produces sub-microsecond precision; Postgres `timestamp` columns store microseconds and round the extra nanos, so instants that round-trip through the database can shift by up to 1µs. That is the only reason `isCloseTo(..., within(1, ChronoUnit.MICROS))` exists (10 call sites in 5 test classes). The fix has two parts: tests use microsecond-aligned fixed instants (they survive Postgres exactly), and the domain reads time from a controllable ambient source instead of calling `Instant.now()` directly.

### 0.5a — `TimeSource` static holder (main sources)

Create `src/main/java/vtertre/infrastructure/time/TimeSource.java`, mirroring the existing `nimnamfood.model.Repositories` service-locator idiom, but defaulting to the system clock so **no production wiring changes are needed**:

```java
package vtertre.infrastructure.time;

import java.time.Instant;
import java.time.InstantSource;

public final class TimeSource {
    private static InstantSource instantSource = InstantSource.system();

    private TimeSource() {
    }

    public static void initialize(InstantSource source) {
        instantSource = source == null ? InstantSource.system() : source;
    }

    public static Instant now() {
        return instantSource.instant();
    }
}
```

Replace the two direct `Instant.now()` calls in the domain with `TimeSource.now()`:
- `src/main/java/nimnamfood/model/recipe/Recipe.java:36`
- `src/main/java/nimnamfood/model/plan/Plan.java:22`

These are the only two `Instant.now()` call sites in `src/main/java` (verified). Do not touch anything else in main sources.

### 0.5b — Test helper: stepping instant source (test sources, Java)

A *fixed* source would break `GetPlansHandlerTest.paginatesThePlansInReversedCreationOrder`, which creates three plans and asserts on reverse creation order — identical timestamps would make the order undefined. Use a source that steps forward on every read, with a microsecond-aligned start and step:

Create `src/test/java/vtertre/infrastructure/time/SteppingInstantSource.java`:

```java
package vtertre.infrastructure.time;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

public class SteppingInstantSource implements InstantSource {
    public static final Instant START = Instant.parse("2026-07-07T12:00:00.123456Z");

    private Instant current = START;

    @Override
    public synchronized Instant instant() {
        final Instant result = current;
        current = current.plus(Duration.ofSeconds(1));
        return result;
    }
}
```

### 0.5c — Rewrite the 10 `isCloseTo` sites to exact equality

Category 1 — the **test** creates the timestamp. Replace `Instant.now()` with a microsecond-aligned literal, then `isCloseTo(x, within(1, ChronoUnit.MICROS))` → `isEqualTo(x)`:

- `PlanJdbcRepositoryTest.retrievesAPlan` (lines 39–40 create, 48–49 assert): use two distinct literals, e.g. `Instant.parse("2026-07-07T12:00:00.123456Z")` and `...13:00:00.123456Z`, so `createdAt`/`updatedAt` stay distinguishable.
- `OnRecipeCreatedFillSearchSummaryTest` (line 40 creates the event, line 49 asserts): literal in the `RecipeCreated` constructor. The other two tests in that class also pass `Instant.now()` to events but never assert on it — replacing those with the literal too is fine and preferred for determinism.
- `RecipeJdbcRepositoryTest.retrievesARecipe` (DBO built in the test, asserted at line 78): literal on the DBO.

Category 2 — the **domain** creates the timestamp (via `Recipe`/`Plan` constructors, now reading `TimeSource`). Add to each affected test class:

```java
@BeforeEach
void setUpTimeSource() {
    TimeSource.initialize(new SteppingInstantSource());
}

@AfterEach
void tearDownTimeSource() {
    TimeSource.initialize(null);
}
```

then rewrite `isCloseTo` → `isEqualTo`:

- `PlanJdbcRepositoryTest.addsAPlan` (lines 67–68)
- `GetPlansHandlerTest` (lines 45, 49) — note the pagination test now relies on the stepping behavior
- `RecipeJdbcRepositoryTest` (line 104 — recipe built via `Recipe.factory()`)
- `OnRecipeChangedUpdateSearchSummaryTest` (line 45)

(A class can need both categories — `PlanJdbcRepositoryTest` and `RecipeJdbcRepositoryTest` do.)

### Verify step 0.5

`mvn verify` green, test count unchanged, and `grep -rn "isCloseTo" src/test` returns nothing. Also confirm `grep -rn "Instant.now()" src/main/java` returns nothing.

Commits (two): `introduire TimeSource pour contrôler l'horloge du domaine` and `remplacer les comparaisons isCloseTo par des égalités strictes`.

During the later migration, the `@BeforeEach`/`@AfterEach` pair above converts to `setup()`/`cleanup()` like any other fixture method (or `TimeSource.initialize(new SteppingInstantSource())` as the first `given:` line — prefer `setup()`/`cleanup()` so the reset is guaranteed).

## Step 1 — Build setup

### pom.xml — properties

```xml
<spock.version>2.4-groovy-4.0</spock.version>
```

### pom.xml — dependencies (test scope), add after `spring-boot-starter-test`

```xml
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-core</artifactId>
    <version>${spock.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-spring</artifactId>
    <version>${spock.version}</version>
    <scope>test</scope>
</dependency>
```

Notes:
- `org.apache.groovy:groovy` needs no version — Spring Boot's parent manages Groovy 4. If the build complains about a missing version, something is wrong; do not pin a Groovy 3 version.
- Spock class-based mocks (needed for Google Cloud Storage classes) require byte-buddy and objenesis; both already arrive transitively through `spring-boot-starter-test` → mockito. Do not remove `spring-boot-starter-test`.

### pom.xml — plugins

```xml
<plugin>
    <groupId>org.codehaus.gmavenplus</groupId>
    <artifactId>gmavenplus-plugin</artifactId>
    <version>4.3.1</version>
    <executions>
        <execution>
            <goals>
                <goal>compileTests</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Spec.java</include>
        </includes>
    </configuration>
</plugin>
```

- `compileTests` compiles `src/test/groovy` by default. Java test sources (the helpers) compile first in the same phase, so Groovy specs can reference them freely. Groovy code must not be referenced *from* Java test code — keep that direction one-way.
- The `**/*Test.java` include stays so the suite keeps running mid-migration; it is still correct at the end (no `*Test` classes will remain).

### Verify step 1

Create `src/test/groovy/nimnamfood/SanitySpec.groovy`:

```groovy
package nimnamfood

import spock.lang.Specification

class SanitySpec extends Specification {
    def "spock runs"() {
        expect:
        1 + 1 == 2
    }
}
```

Run `mvn verify`: baseline count + 1, all green. Delete `SanitySpec.groovy` at the end of step 2. Commit: `mettre en place spock et la compilation groovy des tests`.

## Step 2 — Replace the JUnit test infrastructure

Create these three Groovy base specs. They coexist with the Java originals until step 5 (different class names, no collision).

### 2a. `src/test/groovy/vtertre/infrastructure/persistence/jdbc/PostgresTestContainerBaseSpec.groovy`

```groovy
package vtertre.infrastructure.persistence.jdbc

import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

/* NOTE: For reusing a container (faster test runs), this setup requires the file
 * .testcontainers.properties in your "home" directory (e.g., ~/) to have this property set:
 *
 *      testcontainers.reuse.enable=true
 *
 * You will need to manually clean up running containers.
 */
@DataJdbcTest(properties = "spring.test.database.replace=NONE")
abstract class PostgresTestContainerBaseSpec extends Specification {
    static final PostgreSQLContainer POSTGRESQL_CONTAINER

    static {
        POSTGRESQL_CONTAINER = new PostgreSQLContainer(DockerImageName.parse("postgres:14"))
                .withDatabaseName("postgrestest")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
        POSTGRESQL_CONTAINER.start()
    }

    @DynamicPropertySource
    static void datasourceConfiguration(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl)
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername)
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword)
    }
}
```

Intentional behavior changes, both fine: the unused `@Tag("IO")` is dropped, and `@Testcontainers(disabledWithoutDocker = true)` is dropped (dev machines and CI both have Docker; without it these specs fail instead of being skipped).

### 2b. `src/test/groovy/nimnamfood/infrastructure/repository/memory/MemoryRepositoriesSpec.groovy`

```groovy
package nimnamfood.infrastructure.repository.memory

import nimnamfood.model.Repositories
import spock.lang.Specification

abstract class MemoryRepositoriesSpec extends Specification {
    def setup() {
        Repositories.initialize(new MemoryRepositories())
    }

    def cleanup() {
        Repositories.initialize(null)
    }
}
```

Replaces `@ExtendWith(WithMemoryRepositories.class)`: specs extend this class instead. If a migrated spec needs its own `setup()`, that is fine — Spock runs the superclass `setup()` first and subclass `cleanup()` before the superclass one.

### 2c. `src/test/groovy/nimnamfood/infrastructure/repository/jdbc/JdbcRepositoriesSpec.groovy`

```groovy
package nimnamfood.infrastructure.repository.jdbc

import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcCrudRepository
import nimnamfood.infrastructure.repository.jdbc.plan.PlanJdbcCrudRepository
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeJdbcCrudRepository
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcCrudRepository
import nimnamfood.model.Repositories
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBaseSpec

abstract class JdbcRepositoriesSpec extends PostgresTestContainerBaseSpec {
    @Autowired JdbcAggregateTemplate jdbcAggregateTemplate
    @Autowired TagJdbcCrudRepository tagJdbcCrudRepository
    @Autowired IngredientJdbcCrudRepository ingredientJdbcCrudRepository
    @Autowired RecipeJdbcCrudRepository recipeJdbcCrudRepository
    @Autowired PlanJdbcCrudRepository planJdbcCrudRepository

    def setup() {
        def jdbcRepositories = new JdbcRepositories()
        jdbcRepositories.setJdbcAggregateTemplate(jdbcAggregateTemplate)
        jdbcRepositories.setTagJdbcCrudRepository(tagJdbcCrudRepository)
        jdbcRepositories.setIngredientJdbcCrudRepository(ingredientJdbcCrudRepository)
        jdbcRepositories.setRecipeJdbcCrudRepository(recipeJdbcCrudRepository)
        jdbcRepositories.setPlanJdbcCrudRepository(planJdbcCrudRepository)
        Repositories.initialize(jdbcRepositories)
    }

    def cleanup() {
        Repositories.initialize(null)
    }
}
```

(Spring injects `@Autowired` fields before `setup()` runs — this is guaranteed by spock-spring.)

Commit together with the first migrated specs that use them (steps 3–4), or as `créer les specs de base spock`.

## Step 3 — Conversion recipe (apply to every test class)

For each file: create `<Name>Spec.groovy` in the mirrored path under `src/test/groovy`, converting per the rules below, **delete the original `<Name>Test.java` in the same commit**, run `mvn verify`, check the test count is unchanged.

### Structure

- `class FooTest` → `class FooSpec extends Specification` (or extends the relevant base spec from step 2).
- `@Test void returnsAnEmptyListOfTags()` → `def "returns an empty list of tags"()` — turn the camelCase name into a readable sentence, nothing more.
- Body split: setup code under `given:`, the single tested action under `when:`, assertions under `then:`. For pure computation + assert with no meaningful "when", use `expect:`.
- `@BeforeEach` methods → `def setup()`.
- Keep class-level annotations that belong to Spring: `@Import(...)` stays as-is.
- `@ExtendWith(WithMemoryRepositories.class)` → remove the annotation, `extends MemoryRepositoriesSpec`.
- `@ExtendWith(WithJdbcRepositories.class)` + `extends PostgresTestContainerBase` → `extends JdbcRepositoriesSpec`.
- `extends PostgresTestContainerBase` (alone) → `extends PostgresTestContainerBaseSpec`.
- Private static helper methods at the bottom of test classes: keep them as `private static` methods in the spec.
- Static nested fake classes (`FakeCommand`, `FakeCommandHandler`, `FakeMiddleware`, …) stay as static nested classes inside the spec — Groovy supports them with identical syntax.

### Assertions (AssertJ → Spock conditions)

No AssertJ imports may remain in the Groovy specs.

| AssertJ | Spock condition (in `then:`/`expect:`) |
|---|---|
| `assertThat(x).isEqualTo(y)` | `x == y` |
| `assertThat(x).isNull()` / `.isNotNull()` | `x == null` / `x != null` |
| `assertThat(x).isTrue()` / `.isFalse()` | `x` / `!x` |
| `assertThat(c).hasSize(n)` | `c.size() == n` |
| `assertThat(c).isEmpty()` | `c.isEmpty()` |
| `assertThat(list).containsExactly(a, b)` | `list == [a, b]` |
| `assertThat(set).containsExactly(a)` (on a `Set`) | `set == [a] as Set` |
| `assertThat(c).containsExactlyInAnyOrder(a, b)` | `c as Set == [a, b] as Set && c.size() == 2` |
| `assertThat(c).anyMatch(p)` | `c.any { ... }` |
| `assertThat(x).matches(p)` | inline the predicate as separate conditions |
| `assertThat(opt).isPresent()` / `.isEmpty()` | `opt.isPresent()` / `opt.isEmpty()` |
| `assertThat(c).first().satisfies(e -> {...})` | `with(c.first()) { ... }` (each line inside is a condition) |
| `.asInstanceOf(InstanceOfAssertFactories.type(T.class)).satisfies(...)` | `def e = (T) c.first()` then plain conditions, plus `c.first() instanceof T` |
| `assertThat(t).isCloseTo(u, within(...))` | must not exist anymore after step 0.5 — if encountered, stop and revisit step 0.5 |
| `assertThatExceptionOfType(E.class).isThrownBy(() -> f()).withMessage(m)` | `when: f()` / `then: def e = thrown(E)` and `e.message == m` |

Rules of thumb:
- `thrown()` requires a `when:`/`then:` pair — never put it in `expect:`.
- Java lambdas become closures: `summary -> summary.id().equals(x)` → `{ it.id() == x }`.
- Call record accessors explicitly (`summary.id()`, not `summary.id`) to avoid Groovy property-resolution surprises.

### Mocks (Mockito → Spock)

No Mockito imports should remain, with one permitted exception noted in the per-file table.

| Mockito | Spock |
|---|---|
| `X x = Mockito.mock()` (field) | `X x = Mock()` (field — Spock re-creates it per feature, same semantics as JUnit) |
| `Mockito.when(m.f(a)).thenReturn(v)` | `m.f(a) >> v` — declare in `given:` |
| `Mockito.doThrow(IOException.class).when(m).f(a)` | `m.f(a) >> { throw new IOException() }` in `given:` |
| `Mockito.verify(m, times(1)).f(x)` | `1 * m.f(x)` in `then:` |
| `Mockito.verify(m, never()).f(any())` | `0 * m.f(_)` in `then:` |
| `Mockito.any()` / `any(T.class)` | `_` / `_ as T` |
| `ArgumentCaptor` | capture in an interaction: `def captured = null` then `1 * m.f(_) >> { args -> captured = args[0]; returnValue }`, assert on `captured` afterwards; or match inline: `1 * m.f({ it.name() == "x" })` |

Rules of thumb:
- Stubbing (`>>`) goes in `given:`; verification (`N * …`) goes in `then:`. If you need both a return value and a cardinality check, combine in `then:`: `1 * m.f(a) >> v` (legal because Spock evaluates interactions declared in `then:` before the `when:` runs).
- A stub declared in `given:` and a `1 * …` check in `then:` on the *same call* conflict — use the combined form above instead.

### Groovy pitfalls (read before converting each file)

1. **Double-brace initialization** (`new RecipeIngredientCommandPart() {{ ingredientId = ...; }}`) is Java-only. Replace with `new RecipeIngredientCommandPart().tap { ingredientId = ...; quantity = 20f; unit = IngredientUnit.PIECE }`.
2. **Class-name-dependent assertions**: the bus tests assert exception messages like `"NO_HANDLER_FOUND - class vtertre.infrastructure.bus.command.CommandBusAsyncTest$FakeCommand"`. Renaming the class to `…Spec` changes that string. Build it dynamically: `e.message == "NO_HANDLER_FOUND - " + FakeCommand`. Same pattern in `EventBusAsyncTest` and `QueryBusAsyncTest`.
3. `_1` / `_2` tuple field access (`Tag.factory().create("rapide")._1`) works unchanged in Groovy.
4. Float literals (`20f`) and `Set.of` / `List.of` / `Map.of` work unchanged.
5. Method references (`POSTGRESQL_CONTAINER::getJdbcUrl`) work in Groovy 4.
6. `==` in Groovy is `equals()` — that is what you want. For reference identity use `.is()`.
7. Don't add `where:` blocks, `@Unroll`, `@Shared`, or other Spock features during the migration — 1:1 only.
8. Semicolons and explicit types are legal Groovy; when unsure, keep the Java-ish form rather than golfing.
9. Keep French strings (e.g. `"rapide"`, `"végé"`) byte-for-byte; files must stay UTF-8.

## Step 4 — File-by-file inventory and order

Migrate in this order (each group = one commit, `mvn verify` green + stable test count before committing).

### Group A — pure unit tests (no Spring, no containers) — 13 files

Commit: `migrer les tests unitaires vers spock`

| File (under `src/test/java/`) | Notes |
|---|---|
| `vtertre/command/CommandValidatorTest.java` | plain |
| `vtertre/infrastructure/bus/command/CommandBusAsyncTest.java` | pitfall #2 (class name in message); keep nested fakes |
| `vtertre/infrastructure/bus/command/InvokeCommandHandlerMiddlewareTest.java` | plain |
| `vtertre/infrastructure/bus/query/QueryBusAsyncTest.java` | pitfall #2 |
| `vtertre/infrastructure/bus/event/EventBusAsyncTest.java` | pitfall #2 |
| `vtertre/infrastructure/bus/event/EventPublisherMiddlewareTest.java` | plain |
| `nimnamfood/web/converter/StringToTagFilterQueryConverterTest.java` | plain |
| `nimnamfood/query/recipe/JdbcTagQueryTranslatorTest.java` | plain |
| `nimnamfood/query/ingredient/GetAllIngredientUnitsHandlerTest.java` | plain |
| `nimnamfood/command/recipe/CreateRecipeCommandTest.java` | plain |
| `nimnamfood/command/illustration/ImportIllustrationCommandHandlerTest.java` | Mockito → Spock mocks |
| `nimnamfood/command/illustration/validation/ValidFileValidatorTest.java` | mocks an annotation type + `ConstraintValidatorContext` builder chain; annotations are interfaces, Spock stubs (`annotation.contentTypes() >> fakeContentTypes`) work |
| `nimnamfood/adapter/storage/StorageAdapterTest.java` | mocks GCS `Storage` (interface, fine) and `Blob`/`CopyWriter` (**classes**). Try Spock class mocks first (`Mock(Blob)`). If a class proves unmockable (final methods return real values silently or Spock errors), **keeping Mockito inside this one spec is the permitted fallback** — Groovy calls Mockito fine. Heavy `ArgumentCaptor` use → closure captures |

### Group B — memory-repository tests — 7 files, extend `MemoryRepositoriesSpec`

Commit: `migrer les tests des command handlers vers spock`

| File | Notes |
|---|---|
| `nimnamfood/command/ingredient/CreateIngredientCommandHandlerTest.java` | |
| `nimnamfood/command/ingredient/UpdateIngredientCommandHandlerTest.java` | |
| `nimnamfood/command/tag/CreateTagCommandHandlerTest.java` | |
| `nimnamfood/command/recipe/CreateRecipeCommandHandlerTest.java` | pitfall #1 (double-brace init); `verify(never())` → `0 * recipeService.activateIllustration(_)` |
| `nimnamfood/command/recipe/UpdateRecipeCommandHandlerTest.java` | pitfall #1 likely |
| `nimnamfood/command/plan/GeneratePlanCommandHandlerTest.java` | |
| `nimnamfood/service/RecipeServiceTest.java` | Mockito `MultipartFile` mock + `doThrow(IOException)` + `ArgumentCaptor<Map>` → Spock forms |

### Group C — Postgres/Testcontainers tests — 24 files (needs Docker)

Commit: `migrer les tests d'intégration postgres vers spock` (can be split: repositories / queries / projections)

All extend `PostgresTestContainerBaseSpec`, except `OnPlanCreatedFillSummaryTest` which extends `JdbcRepositoriesSpec`. Keep `@Import(...)` and `@Autowired` fields as-is (Groovy syntax: `@Autowired TagsViewTestHelper view`).

Repositories: `nimnamfood/infrastructure/repository/jdbc/{Ingredient,Plan,Recipe,Tag}JdbcRepositoryTest.java` (Plan and Recipe carry the `TimeSource`/`SteppingInstantSource` fixture from step 0.5 — convert it to `setup()`/`cleanup()`), `vtertre/infrastructure/persistence/jdbc/JdbcRepositoryTest.java` (uses the Java `Fake*` helpers — those stay in Java).

Query handlers: `nimnamfood/query/tag/FindTagsHandlerTest.java`, `nimnamfood/query/ingredient/FindIngredientsHandlerTest.java`, `nimnamfood/query/recipe/{FindRecipes,GetRecipe}HandlerTest.java`, `nimnamfood/query/plan/{GetPlan,GetPlans}HandlerTest.java` (`GetPlansHandlerTest` carries the step 0.5 clock fixture).

Projections (all under `…/projection/`): 2 ingredient, 2 plan (one is the `JdbcRepositoriesSpec` user), 8 recipe, 1 tag — 13 files. Several create mocks with `Mockito.mock()` + `Mockito.when(...)` for `RecipeService` → `RecipeService recipeService = Mock()` with `recipeService.illustrationUrl(x) >> "url"` in `given:`. `OnRecipeChangedUpdateSearchSummaryTest` carries the step 0.5 clock fixture.

### Helpers that must NOT be migrated (stay in `src/test/java`)

`ObjectMapperFactory`, `TagsViewTestHelper`, `IngredientsViewTestHelper`, `RecipesViewTestHelper`, `RecipeSearchViewTestHelper`, `PlansViewTestHelper`, `PlanSearchViewTestHelper`, the 6 `nimnamfood/query/recipe/model/*Inspector` records, the 4 `vtertre/infrastructure/persistence/jdbc/Fake*` classes, and `SteppingInstantSource` (created in step 0.5).

## Step 5 — Cleanup

1. Delete the now-unused JUnit infrastructure: `PostgresTestContainerBase.java`, `WithMemoryRepositories.java`, `WithJdbcRepositories.java`.
2. Remove the `org.testcontainers:junit-jupiter` dependency from `pom.xml` (its annotations are no longer used; `org.testcontainers:postgresql` pulls testcontainers core itself). Keep `org.testcontainers:postgresql`. Keep `spring-boot-starter-test` (provides spring-test used by spock-spring, plus byte-buddy/objenesis for class mocks).
3. `grep -r "org.junit" src/test/groovy` and `grep -r "assertj" src/test/groovy` must return nothing; `find src/test/java -name "*Test.java"` must return nothing.
4. Final `mvn verify` (Docker running): green, test count equal to the step 0 baseline.
5. Commit: `supprimer l'infrastructure de test junit`.

## Acceptance criteria

- [ ] Step 0.5 done first: `TimeSource` in place, no `Instant.now()` in `src/main/java`, no `isCloseTo` anywhere in `src/test`.
- [ ] All 44 test classes exist as `*Spec.groovy` under `src/test/groovy`; no `*Test.java` remains.
- [ ] Helper classes untouched in `src/test/java`.
- [ ] `mvn verify` green with the same test count as the baseline.
- [ ] No JUnit, AssertJ, or Mockito imports in Groovy specs (single allowed exception: Mockito in `StorageAdapterSpec` if Spock class mocks fail on GCS classes — leave a short comment there if so).
- [ ] CI (`mvn verify`) unchanged and green.
