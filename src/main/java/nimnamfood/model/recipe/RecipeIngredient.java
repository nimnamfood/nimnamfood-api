package nimnamfood.model.recipe;

import nimnamfood.model.ingredient.Ingredient;

public record RecipeIngredient(Ingredient ingredient, float quantity, boolean quantityFixed) {
}
