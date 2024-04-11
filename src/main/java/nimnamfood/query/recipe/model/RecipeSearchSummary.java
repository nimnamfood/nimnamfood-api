package nimnamfood.query.recipe.model;

import nimnamfood.query.tag.model.TagSummary;

import java.util.Set;
import java.util.UUID;

public record RecipeSearchSummary(UUID id, String name, String illustrationUrl, Set<TagSummary> tags) {
}
