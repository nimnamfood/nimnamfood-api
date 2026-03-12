package nimnamfood.infrastructure.repository.jdbc.plan;

import com.google.common.collect.Sets;
import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.Plan;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import vtertre.infrastructure.persistence.jdbc.BaseJdbcDboWithUuid;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Table("plans")
public class PlanDbo extends BaseJdbcDboWithUuid<Plan> {
    Instant createdAt;
    Instant updatedAt;

    @MappedCollection(idColumn = "plan_id")
    Set<MealDbo> meals = Sets.newHashSet();

    @Override
    public Plan asAggregateRoot() {
        final Set<Meal> meals = this.meals
                .stream()
                .map(mealDbo -> new Meal(mealDbo.id, mealDbo.mealIndex, mealDbo.recipeId))
                .collect(Collectors.toSet());
        return new Plan(this.getId(), this.createdAt, this.updatedAt, meals);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<MealDbo> meals() {
        return meals;
    }

    public void setMeals(Set<MealDbo> meals) {
        this.meals = meals;
    }
}
