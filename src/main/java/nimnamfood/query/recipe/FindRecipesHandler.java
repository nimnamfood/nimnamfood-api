package nimnamfood.query.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.QueryNormalizer;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.stream.Stream;

@Component
public class FindRecipesHandler implements QueryHandler<FindRecipes, List<RecipeSearchSummary>> {
    @Override
    public List<RecipeSearchSummary> execute(FindRecipes query) {
        Stream<Recipe> sourceStream = Repositories.recipes().getAll().stream();
        Stream<Recipe> filteredStream = query.query == null ? sourceStream : sourceStream.filter(recipe ->
                QueryNormalizer.normalize(recipe.getName()).contains(QueryNormalizer.normalize(query.query)));

        return filteredStream
                .map(RecipeSearchSummary::fromRecipe)
                .toList();
    }
}
