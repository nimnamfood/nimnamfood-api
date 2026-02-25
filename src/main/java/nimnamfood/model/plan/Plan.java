package nimnamfood.model.plan;

import vtertre.ddd.BaseAggregateRootWithUuid;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

public class Plan extends BaseAggregateRootWithUuid {
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Set<Meal> meals;

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
