package nimnamfood.model.plan;

import vtertre.ddd.BaseEntityWithUuid;

import java.util.UUID;

public class Meal extends BaseEntityWithUuid {
    private final int mealIndex;
    private final UUID recipeId;

    public Meal(int mealIndex, UUID recipeId) {
        this.mealIndex = mealIndex;
        this.recipeId = recipeId;
    }

    public Meal(UUID id, int mealIndex, UUID recipeId) {
        super(id);
        this.mealIndex = mealIndex;
        this.recipeId = recipeId;
    }

    public int mealIndex() {
        return this.mealIndex;
    }

    public UUID recipeId() {
        return this.recipeId;
    }
}
