package nimnamfood.model.recipe;

import nimnamfood.model.ingredient.IngredientUnit;
import vtertre.ddd.BaseEntityWithUuid;

import java.util.UUID;

public class RecipeIngredient extends BaseEntityWithUuid {
    private final UUID ingredientId;
    private final float quantity;
    private final IngredientUnit unit;

    public RecipeIngredient(UUID ingredientId, float quantity, IngredientUnit unit) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
    }

    public RecipeIngredient(UUID id, UUID ingredientId, float quantity, IngredientUnit unit) {
        super(id);
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
    }

    public UUID ingredientId() {
        return ingredientId;
    }

    public float quantity() {
        return quantity;
    }

    public IngredientUnit unit() {
        return unit;
    }

}
