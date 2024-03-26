package nimnamfood.query.recipe;

import nimnamfood.query.recipe.model.RecipeSearchSummary;
import vtertre.query.Query;

import java.util.List;
import java.util.Set;

public class FindRecipes extends Query<List<RecipeSearchSummary>> {
    public final String query;
    public final Set<String> tags;

    public FindRecipes(String query, Set<String> tags) {
        this.query = query;
        this.tags = tags;
    }

    public FindRecipes(String query) {
        this.query = query;
        this.tags = null;
    }

    public FindRecipes(Set<String> tags) {
        this.query = null;
        this.tags = tags;
    }

    public FindRecipes() {
        this.query = null;
        this.tags = null;
    }
}
