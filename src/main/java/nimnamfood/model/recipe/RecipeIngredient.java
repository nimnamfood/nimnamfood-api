package nimnamfood.model.recipe;

import nimnamfood.model.ingredient.IngredientUnit;

import java.util.UUID;

public record RecipeIngredient(UUID ingredientId, float quantity, IngredientUnit unit, boolean quantityFixed) {
}
