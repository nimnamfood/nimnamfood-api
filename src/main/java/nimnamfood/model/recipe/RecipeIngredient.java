package nimnamfood.model.recipe;

import java.util.UUID;

public record RecipeIngredient(UUID ingredientId, float quantity, boolean quantityFixed) {
}
