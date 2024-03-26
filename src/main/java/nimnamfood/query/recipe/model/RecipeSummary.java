package nimnamfood.query.recipe.model;

import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.tag.model.TagSummary;

import java.util.Set;
import java.util.UUID;

public class RecipeSummary {
    public UUID id;
    public String name;
    public int portionsCount;
    public String instructions;
    public Set<RecipeIngredientSummary> ingredients;
    public Set<TagSummary> tags;

    public static RecipeSummary fromRecipe(Recipe recipe, Set<RecipeIngredientSummary> ingredients,
                                           Set<TagSummary> tags) {
        final RecipeSummary summary = new RecipeSummary();
        summary.id = recipe.getId();
        summary.name = recipe.getName();
        summary.portionsCount = recipe.getPortionsCount();
        summary.instructions = recipe.getInstructions();
        summary.ingredients = ingredients;
        summary.tags = tags;
        return summary;
    }
}
