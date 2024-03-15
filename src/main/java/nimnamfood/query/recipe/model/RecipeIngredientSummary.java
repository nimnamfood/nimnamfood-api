package nimnamfood.query.recipe.model;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.RecipeIngredient;

import java.util.UUID;

public class RecipeIngredientSummary {
    public UUID id;
    public String name;
    public IngredientUnit unit;
    public float quantity;
    public boolean quantityFixed;

    public static RecipeIngredientSummary fromRecipeIngredient(RecipeIngredient recipeIngredient, Ingredient ingredient) {
        RecipeIngredientSummary summary = new RecipeIngredientSummary();
        summary.id = recipeIngredient.ingredientId();
        summary.name = ingredient.getName();
        summary.unit = ingredient.getUnit();
        summary.quantity = recipeIngredient.quantity();
        summary.quantityFixed = recipeIngredient.quantityFixed();
        return summary;
    }
}
