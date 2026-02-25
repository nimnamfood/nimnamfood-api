package nimnamfood.infrastructure.repository.jdbc.plan;

import com.google.common.collect.Sets;
import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.Plan;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import vtertre.infrastructure.persistence.jdbc.BaseJdbcDboWithUuid;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Table("plans")
public class PlanDbo extends BaseJdbcDboWithUuid<Plan> {
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

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

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<MealDbo> meals() {
        return meals;
    }

    public void setMeals(Set<MealDbo> meals) {
        this.meals = meals;
    }
}
