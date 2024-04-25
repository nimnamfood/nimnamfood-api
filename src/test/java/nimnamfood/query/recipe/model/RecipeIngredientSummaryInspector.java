package nimnamfood.query.recipe.model;

import nimnamfood.model.ingredient.IngredientUnit;

import java.util.UUID;

public record RecipeIngredientSummaryInspector(UUID id, String name, float quantity, IngredientUnit unit,
                                               boolean quantityFixed) {
}
