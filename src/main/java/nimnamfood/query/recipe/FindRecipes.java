package nimnamfood.query.recipe;

import nimnamfood.query.recipe.model.RecipeSearchSummary;
import vtertre.query.Query;

import java.util.List;

public class FindRecipes implements Query<List<RecipeSearchSummary>> {
    public final String query;

    public FindRecipes() {
        this.query = null;
    }

    public FindRecipes(String query) {
        this.query = query;
    }
}
