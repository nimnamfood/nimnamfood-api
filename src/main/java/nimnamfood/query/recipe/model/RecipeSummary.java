package nimnamfood.query.recipe.model;

import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.tag.model.TagSummary;

import java.util.List;
import java.util.UUID;

public class RecipeSummary {
    public UUID id;
    public String name;
    public int portionsCount;
    public String instructions;
    public List<RecipeIngredientSummary> ingredients;
    public List<TagSummary> tags;

    public static RecipeSummary fromRecipe(Recipe recipe, List<RecipeIngredientSummary> ingredients,
                                           List<TagSummary> tags) {
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
