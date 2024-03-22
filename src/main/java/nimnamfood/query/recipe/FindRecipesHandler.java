package nimnamfood.query.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.QueryNormalizer;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.stereotype.Component;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.query.QueryHandler;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Component
public class FindRecipesHandler implements QueryHandler<FindRecipes, List<RecipeSearchSummary>> {
    @Override
    public List<RecipeSearchSummary> execute(FindRecipes query) {
        return Repositories.recipes()
                .getAll(matchesQueryAndTags(query))
                .stream()
                .map(recipe -> {
                    final List<TagSummary> tags = tagSummaries(recipe.getTagIds());
                    return RecipeSearchSummary.fromRecipe(recipe, tags);
                })
                .toList();
    }

    private static Predicate<Recipe> matchesQueryAndTags(FindRecipes query) {
        return recipe -> {
            final boolean queryMatches = query.query == null || QueryNormalizer.partialMatch(recipe.getName(), query.query);
            final boolean tagsMatch = query.tags == null || new HashSet<>(
                    recipe.getTagIds().stream().map(UUID::toString).toList()).containsAll(query.tags);
            return queryMatches && tagsMatch;
        };
    }

    private static List<TagSummary> tagSummaries(List<UUID> tagIds) {
        return tagIds.stream()
                .map(id -> Repositories.tags().get(id)
                        .orElseThrow(() -> new MissingAggregateRootException(id)))
                .map(TagSummary::fromTag)
                .toList();
    }
}
