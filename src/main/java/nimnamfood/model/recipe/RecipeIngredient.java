package nimnamfood.model.recipe;

import nimnamfood.model.ingredient.IngredientUnit;
import vtertre.ddd.BaseEntityWithUuid;

import java.util.UUID;

public class RecipeIngredient extends BaseEntityWithUuid {
    private final UUID ingredientId;
    private final float quantity;
    private final IngredientUnit unit;
    private final boolean quantityFixed;

    public RecipeIngredient(UUID ingredientId, float quantity, IngredientUnit unit, boolean quantityFixed) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
        this.quantityFixed = quantityFixed;
    }

    public RecipeIngredient(UUID id, UUID ingredientId, float quantity, IngredientUnit unit, boolean quantityFixed) {
        super(id);
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
        this.quantityFixed = quantityFixed;
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

    public boolean quantityFixed() {
        return quantityFixed;
    }
}
