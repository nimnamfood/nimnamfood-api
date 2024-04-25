package nimnamfood.query.recipe.model;

import java.util.Set;
import java.util.UUID;

public record RecipeSummaryInspector(UUID id, String name, RecipeIllustrationSummaryInspector illustration,
                                     int portionsCount, String instructions,
                                     Set<RecipeIngredientSummaryInspector> ingredients,
                                     Set<RecipeTagSummaryPartInspector> tags) {
}
