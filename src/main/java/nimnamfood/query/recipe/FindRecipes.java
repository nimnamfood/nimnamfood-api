package nimnamfood.query.recipe;

import nimnamfood.query.recipe.model.RecipeSearchSummary;
import vtertre.query.Query;

import java.util.List;

public class FindRecipes extends Query<List<RecipeSearchSummary>> {
    public final String query;
    public final List<String> tags;

    public FindRecipes(String query, List<String> tags) {
        this.query = query;
        this.tags = tags;
    }

    public FindRecipes(String query) {
        this.query = query;
        this.tags = null;
    }

    public FindRecipes(List<String> tags) {
        this.query = null;
        this.tags = tags;
    }

    public FindRecipes() {
        this.query = null;
        this.tags = null;
    }
}
