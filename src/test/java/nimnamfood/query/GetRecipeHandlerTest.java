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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith({WithMemoryRepositories.class})
public class GetRecipeHandlerTest {

    @Test
    void returnsTheRecipeWithMatchingId() {
        GetRecipeHandler handler = new GetRecipeHandler();
        Ingredient ingredient = new Ingredient("ingredient", IngredientUnit.GRAM);
        Tag tag = new Tag("tag");
        Repositories.ingredients().add(ingredient);
        Repositories.tags().add(tag);
        Recipe recipe = RecipeFactory.create("recette 1", ingredient, tag);
        Repositories.recipes().add(recipe);
        Repositories.recipes().add(RecipeFactory.createEmpty("recette 2"));

        RecipeSummary summary = handler.execute(new GetRecipe(recipe.getId().toString()));

        assertThat(summary.id).isEqualTo(recipe.getId());
        assertThat(summary.name).isEqualTo(recipe.getName());
        assertThat(summary.portionsCount).isEqualTo(recipe.getPortionsCount());
        assertThat(summary.instructions).isEqualTo(recipe.getInstructions());
        assertThat(summary.tags.stream().findFirst().get().name).isEqualTo("tag");
        assertThat(summary.ingredients.stream().findFirst().get().name).isEqualTo("ingredient");
        assertThat(summary.ingredients.stream().findFirst().get().unit).isEqualTo(IngredientUnit.PIECE);
    }

    @Test
    void throwsAnExceptionWhenTheProvidedIdDoesNotMatchAnyEntity() {
        GetRecipeHandler handler = new GetRecipeHandler();
        String stringUuid = UUID.randomUUID().toString();

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(new GetRecipe(stringUuid)))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + stringUuid);
    }

    @Test
    void throwsAnExceptionWhenATagReferenceIdDoesNotMatchAnyEntity() {
        GetRecipeHandler handler = new GetRecipeHandler();
        Tag tag = new Tag("tag");
        Recipe recipe = Recipe.factory().create("", 1, Collections.emptySet(), "", Set.of(tag.getId()));
        Repositories.recipes().add(recipe);

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(new GetRecipe(recipe.getId().toString())))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + tag.getId().toString());
    }

    @Test
    void throwsAnExceptionWhenAnIngredientReferenceIdDoesNotMatchAnyEntity() {
        GetRecipeHandler handler = new GetRecipeHandler();
        Ingredient ingredient = new Ingredient("ingredient", IngredientUnit.GRAM);
        Recipe recipe = Recipe.factory().create("", 1,
                Set.of(new RecipeIngredient(ingredient.getId(), 1, IngredientUnit.GRAM, false)), "", Collections.emptySet());
        Repositories.recipes().add(recipe);

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(new GetRecipe(recipe.getId().toString())))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + ingredient.getId().toString());
    }

    private static class RecipeFactory {
        public static Recipe createEmpty(String name) {
            return Recipe.factory().create(name, 1, Collections.emptySet(), "instructions", Collections.emptySet());
        }

        public static Recipe create(String name, Ingredient ingredient, Tag tag) {
            RecipeIngredient recipeIngredient = new RecipeIngredient(ingredient.getId(), 120, IngredientUnit.PIECE, false);
            return Recipe.factory().create(name, 1, Set.of(recipeIngredient), "instructions", Set.of(tag.getId()));
        }
    }
}
