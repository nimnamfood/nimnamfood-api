package nimnamfood.query.recipe.model;

import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.tag.model.TagSummary;

import java.util.Set;
import java.util.UUID;

public class RecipeSearchSummary {
    public UUID id;
    public String name;
    public Set<TagSummary> tags;

    public static RecipeSearchSummary fromRecipe(Recipe recipe, Set<TagSummary> tags) {
        final RecipeSearchSummary summary = new RecipeSearchSummary();
        summary.id = recipe.getId();
        summary.name = recipe.getName();
        summary.tags = tags;
        return summary;
    }
}
