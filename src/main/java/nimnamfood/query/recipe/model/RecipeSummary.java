package nimnamfood.query.recipe.model;

import nimnamfood.query.tag.model.TagSummary;

import java.util.Set;
import java.util.UUID;

public record RecipeSummary(
        UUID id, String name, int portionsCount, String instructions, Set<RecipeIngredientSummary> ingredients,
        Set<TagSummary> tags) {
}
