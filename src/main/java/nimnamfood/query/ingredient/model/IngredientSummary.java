package nimnamfood.query.ingredient.model;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;

import java.util.UUID;

public class IngredientSummary {
    public UUID id;
    public String name;
    public IngredientUnit unit;

    public static IngredientSummary fromIngredient(Ingredient ingredient) {
        final IngredientSummary summary = new IngredientSummary();
        summary.id = ingredient.getId();
        summary.name = ingredient.getName();
        summary.unit = ingredient.getUnit();
        return summary;
    }
}
