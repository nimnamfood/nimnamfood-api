package nimnamfood.query.ingredient.model;

import nimnamfood.model.ingredient.IngredientUnit;

import java.util.UUID;

public record IngredientSummary(UUID id, String name, IngredientUnit unit) {
}
