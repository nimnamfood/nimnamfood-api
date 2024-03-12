package nimnamfood.query.recipe.model;

import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
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

    public static RecipeSummary fromRecipe(Recipe recipe) {
        final RecipeSummary summary = new RecipeSummary();
        summary.id = recipe.getId();
        summary.name = recipe.getName();
        summary.portionsCount = recipe.getPortionsCount();
        summary.instructions = recipe.getInstructions();
        summary.ingredients = recipe.getIngredients().stream().map(RecipeIngredientSummary::fromRecipeIngredient).toList();
        summary.tags = recipe.getTags().stream().map(TagSummary::fromTag).toList();
        return summary;
    }

    public static class RecipeIngredientSummary {
        public UUID id;
        public String name;
        public IngredientUnit unit;
        public float quantity;
        public boolean quantityFixed;

        private static RecipeIngredientSummary fromRecipeIngredient(RecipeIngredient recipeIngredient) {
            RecipeIngredientSummary summary = new RecipeIngredientSummary();
            summary.id = recipeIngredient.ingredient().getId();
            summary.name = recipeIngredient.ingredient().getName();
            summary.unit = recipeIngredient.ingredient().getUnit();
            summary.quantity = recipeIngredient.quantity();
            summary.quantityFixed = recipeIngredient.quantityFixed();
            return summary;
        }
    }
}
