package nimnamfood.query.recipe.model;

import nimnamfood.model.ingredient.IngredientUnit;

import java.util.UUID;

public class RecipeIngredientSummary {
    public UUID id;
    public String name;
    public float quantity;
    public IngredientUnit unit;
    public boolean quantityFixed;
}
