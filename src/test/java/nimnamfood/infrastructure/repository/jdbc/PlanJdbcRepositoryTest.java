package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.jdbc.plan.MealDbo;
import nimnamfood.infrastructure.repository.jdbc.plan.PlanDbo;
import nimnamfood.infrastructure.repository.jdbc.plan.PlanJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.plan.PlanJdbcRepository;
import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.Plan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class PlanJdbcRepositoryTest extends PostgresTestContainerBase {
    @Autowired
    PlanJdbcCrudRepository crudRepository;
    @Autowired
    JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void retrievesAPlan() {
        PlanJdbcRepository repository = new PlanJdbcRepository(crudRepository, jdbcAggregateTemplate);
        MealDbo mealDbo = new MealDbo();
        mealDbo.setId(UUID.randomUUID());
        mealDbo.setMealIndex(0);
        mealDbo.setRecipeId(UUID.randomUUID());

        PlanDbo planDbo = new PlanDbo();
        planDbo.setId(UUID.randomUUID());
        planDbo.setCreatedAt(Instant.now());
        planDbo.setUpdatedAt(Instant.now());
        planDbo.setMeals(Set.of(mealDbo));
        this.jdbcAggregateTemplate.insert(planDbo);

        Optional<Plan> foundPlan = repository.get(planDbo.getId());

        assertThat(foundPlan).isPresent();
        assertThat(foundPlan.get().getId()).isEqualTo(planDbo.getId());
        assertThat(foundPlan.get().createdAt()).isCloseTo(planDbo.createdAt(), within(1, ChronoUnit.MICROS));
        assertThat(foundPlan.get().updatedAt()).isCloseTo(planDbo.updatedAt(), within(1, ChronoUnit.MICROS));

        Meal meal = foundPlan.get().meals().stream().findFirst().get();
        assertThat(meal.getId()).isEqualTo(mealDbo.id());
        assertThat(meal.mealIndex()).isEqualTo(mealDbo.mealIndex());
        assertThat(meal.recipeId()).isEqualTo(mealDbo.recipeId());
    }

    @Test
    void addsAPlan() {
        PlanJdbcRepository repository = new PlanJdbcRepository(crudRepository, jdbcAggregateTemplate);
        Meal meal = new Meal(0, UUID.randomUUID());
        Plan plan = new Plan(Set.of(meal));

        repository.add(plan);
        PlanDbo dbo = this.jdbcAggregateTemplate.findById(plan.getId(), PlanDbo.class);

        assertThat(dbo).isNotNull();
        assertThat(dbo.createdAt()).isCloseTo(plan.createdAt(), within(1, ChronoUnit.MICROS));
        assertThat(dbo.updatedAt()).isCloseTo(plan.updatedAt(), within(1, ChronoUnit.MICROS));

        MealDbo mealDbo = dbo.meals().stream().findFirst().get();
        assertThat(mealDbo.id()).isEqualTo(meal.getId());
        assertThat(mealDbo.mealIndex()).isEqualTo(meal.mealIndex());
        assertThat(mealDbo.recipeId()).isEqualTo(meal.recipeId());
    }
}
