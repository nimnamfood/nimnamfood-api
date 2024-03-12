package nimnamfood.query;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.recipe.GetRecipe;
import nimnamfood.query.recipe.GetRecipeHandler;
import nimnamfood.query.recipe.model.RecipeSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.MissingAggregateRootException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith({WithMemoryRepositories.class})
public class GetRecipeHandlerTest {

    @Test
    void returnsTheRecipeWithMatchingId() {
        GetRecipeHandler handler = new GetRecipeHandler();
        Recipe recipe = RecipeFactory.create("recette 1");
        Repositories.recipes().add(recipe);
        Repositories.recipes().add(RecipeFactory.createEmpty("recette 2"));

        RecipeSummary summary = handler.execute(new GetRecipe(recipe.getId().toString()));

        assertThat(summary.id).isEqualTo(recipe.getId());
        assertThat(summary.name).isEqualTo(recipe.getName());
        assertThat(summary.portionsCount).isEqualTo(recipe.getPortionsCount());
        assertThat(summary.instructions).isEqualTo(recipe.getInstructions());
        assertThat(summary.tags.getFirst().name).isEqualTo("tag");
        assertThat(summary.ingredients.getFirst().name).isEqualTo("ingredient");
    }

    @Test
    void throwsAnExceptionWhenTheProvidedIdDoesNotMatchAnyEntity() {
        GetRecipeHandler handler = new GetRecipeHandler();
        String stringUuid = UUID.randomUUID().toString();

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(new GetRecipe(stringUuid)))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + stringUuid);
    }

    private static class RecipeFactory {
        public static Recipe createEmpty(String name) {
            return Recipe.factory().create(name, 1, Collections.emptyList(), "instructions", Collections.emptyList());
        }

        public static Recipe create(String name) {
            Ingredient ingredient = new Ingredient("ingredient", IngredientUnit.GRAM);
            RecipeIngredient recipeIngredient = new RecipeIngredient(ingredient, 120, false);
            Tag tag = new Tag("tag");
            return Recipe.factory().create(name, 1, List.of(recipeIngredient), "instructions", List.of(tag));
        }
    }
}
