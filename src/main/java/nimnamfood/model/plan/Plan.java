package nimnamfood.model.plan;

import com.google.common.collect.ImmutableSet;
import vtertre.ddd.BaseAggregateRootWithUuid;
import vtertre.ddd.BusinessError;
import vtertre.ddd.Tuple;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

public class Plan extends BaseAggregateRootWithUuid {
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Set<Meal> meals;

    public static Factory factory() {
        return new Factory();
    }

    public Plan(Set<Meal> meals) {
        final LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        this.createdAt = localDateTime;
        this.updatedAt = localDateTime;
        this.meals = meals;
    }

    public Plan(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt, Set<Meal> meals) {
        super(id);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.meals = meals;
    }

    public static class Factory {
        public Tuple<Plan, PlanCreated> create(Set<Meal> meals) {
            final var distinctCount = meals.stream().map(Meal::mealIndex).distinct().count();
            if (distinctCount != meals.size()) {
                throw new BusinessError("DUPLICATE_MEAL_INDEX");
            }

            final var plan = new Plan(meals);
            return Tuple.of(plan, new PlanCreated(plan.getId(), plan.createdAt, ImmutableSet.copyOf(plan.meals)));
        }
    }

    public LocalDateTime createdAt() {
        return this.createdAt;
    }

    public LocalDateTime updatedAt() {
        return this.updatedAt;
    }

    public Set<Meal> meals() {
        return this.meals;
    }
}
