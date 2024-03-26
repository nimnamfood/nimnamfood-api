package nimnamfood.query.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.query.recipe.model.RecipeIngredientSummary;
import nimnamfood.query.recipe.model.RecipeSummary;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.stereotype.Component;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GetRecipeHandler implements QueryHandler<GetRecipe, RecipeSummary> {
    @Override
    public RecipeSummary execute(GetRecipe query) {
        return Repositories.recipes()
                .get(query.id)
                .map(recipe -> {
                    final Set<TagSummary> tags = this.tagSummaries(recipe.getTagIds());
                    final Set<RecipeIngredientSummary> ingredients = this.ingredientSummaries(recipe.getIngredients());
                    return RecipeSummary.fromRecipe(recipe, ingredients, tags);
                })
                .orElseThrow(() -> new MissingAggregateRootException(query.id));
    }

    private Set<RecipeIngredientSummary> ingredientSummaries(Set<RecipeIngredient> recipeIngredients) {
        return recipeIngredients.stream()
                .map(recipeIngredient -> {
                    final Ingredient ingredient = Repositories.ingredients().get(recipeIngredient.ingredientId())
                            .orElseThrow(() -> new MissingAggregateRootException(recipeIngredient.ingredientId()));
                    return RecipeIngredientSummary.fromRecipeIngredient(recipeIngredient, ingredient);
                })
                .collect(Collectors.toSet());
    }

    private Set<TagSummary> tagSummaries(Set<UUID> tagIds) {
        return tagIds.stream()
                .map(id -> Repositories.tags().get(id)
                        .orElseThrow(() -> new MissingAggregateRootException(id)))
                .map(TagSummary::fromTag)
                .collect(Collectors.toSet());
    }
}
