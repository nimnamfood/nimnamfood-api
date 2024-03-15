package nimnamfood.query.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.QueryNormalizer;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.stereotype.Component;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class FindRecipesHandler implements QueryHandler<FindRecipes, List<RecipeSearchSummary>> {
    @Override
    public List<RecipeSearchSummary> execute(FindRecipes query) {
        Stream<Recipe> sourceStream = Repositories.recipes().getAll().stream();
        Stream<Recipe> filteredStream = query.query == null ? sourceStream : sourceStream.filter(recipe ->
                QueryNormalizer.normalize(recipe.getName()).contains(QueryNormalizer.normalize(query.query)));

        return filteredStream
                .map(recipe -> {
                    final List<TagSummary> tags = this.tagSummaries(recipe.getTagIds());
                    return RecipeSearchSummary.fromRecipe(recipe, tags);
                })
                .toList();
    }

    private List<TagSummary> tagSummaries(List<UUID> tagIds) {
        return tagIds.stream()
                .map(id -> Repositories.tags().get(id)
                        .orElseThrow(() -> new MissingAggregateRootException(id)))
                .map(TagSummary::fromTag)
                .toList();
    }
}
