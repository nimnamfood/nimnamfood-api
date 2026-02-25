package nimnamfood.infrastructure.repository.jdbc.plan;

import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.Plan;
import nimnamfood.model.plan.PlanRepository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.JdbcRepositoryWithUuid;

import java.util.stream.Collectors;

public class PlanJdbcRepository extends JdbcRepositoryWithUuid<Plan, PlanDbo> implements PlanRepository {

    public PlanJdbcRepository(PlanJdbcCrudRepository jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        super(jdbcCrudRepository, jdbcAggregateTemplate);
    }

    @Override
    public PlanDbo toDbo(Plan plan) {
        final PlanDbo dbo = new PlanDbo();
        dbo.setId(plan.getId());
        dbo.createdAt = plan.createdAt();
        dbo.updatedAt = plan.updatedAt();
        dbo.meals = plan.meals().stream().map(PlanJdbcRepository::mealDbo).collect(Collectors.toSet());
        return dbo;
    }

    private static MealDbo mealDbo(Meal meal) {
        final MealDbo mealDbo = new MealDbo();
        mealDbo.id = meal.getId();
        mealDbo.mealIndex = meal.mealIndex();
        mealDbo.recipeId = meal.recipeId();
        return mealDbo;
    }
}
