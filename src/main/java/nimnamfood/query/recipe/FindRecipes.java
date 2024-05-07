package nimnamfood.query.recipe;

import nimnamfood.query.recipe.model.RecipeSearchSummary;
import nimnamfood.web.converter.TagFilterQuery;
import vtertre.query.Query;

import java.util.List;

public class FindRecipes extends Query<List<RecipeSearchSummary>> {
    public final String query;
    public final TagFilterQuery tagFilterQuery;

    public FindRecipes(String query, TagFilterQuery tagFilterQuery) {
        this.query = query;
        this.tagFilterQuery = tagFilterQuery;
    }

    public FindRecipes(String query) {
        this.query = query;
        this.tagFilterQuery = null;
    }

    public FindRecipes(TagFilterQuery tagFilterQuery) {
        this.query = null;
        this.tagFilterQuery = tagFilterQuery;
    }

    public FindRecipes() {
        this.query = null;
        this.tagFilterQuery = null;
    }
}
