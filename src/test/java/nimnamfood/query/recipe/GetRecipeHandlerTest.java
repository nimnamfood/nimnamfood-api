package nimnamfood.query.recipe;

import nimnamfood.infrastructure.repository.jdbc.WithJdbcRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.recipe.model.RecipeSummary;
import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith({WithJdbcRepositories.class})
public class GetRecipeHandlerTest extends PostgresTestContainerBase {
    @Autowired
    NamedParameterJdbcTemplate template;

    RecipeService recipeService = Mockito.mock();

    @Test
    void returnsTheRecipeWithMatchingId() {
        GetRecipeHandler handler = new GetRecipeHandler(recipeService);
        Ingredient ingredient1 = Ingredient.factory().create("ingredient 1", IngredientUnit.GRAM)._1;
        Ingredient ingredient2 = Ingredient.factory().create("ingredient 2", IngredientUnit.PIECE)._1;
        Tag tag1 = Tag.factory().create("tag 1")._1;
        Tag tag2 = Tag.factory().create("tag 2")._1;
        Repositories.ingredients().add(ingredient1);
        Repositories.ingredients().add(ingredient2);
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);
        Recipe recipe = Recipe.factory().create("recette 1", UUID.randomUUID(), 1, Set.of(new RecipeIngredient(ingredient1.getId(), 10f, IngredientUnit.PINCH, false), new RecipeIngredient(ingredient2.getId(), 5f, IngredientUnit.GRAM, true)), "instructions", Set.of(tag1.getId(), tag2.getId()))._1;
        Repositories.recipes().add(recipe);
        Repositories.recipes().add(RecipeFactory.createEmpty("recette 2"));
        Mockito.when(recipeService.illustrationUrl(recipe.getIllustrationId())).thenReturn("url");

        RecipeSummary summary = handler.execute(new GetRecipe(recipe.getId().toString()), template);

        assertThat(summary.id()).isEqualTo(recipe.getId());
        assertThat(summary.name()).isEqualTo(recipe.getName());
        assertThat(summary.illustration().id()).isEqualTo(recipe.getIllustrationId());
        assertThat(summary.illustration().url()).isEqualTo("url");
        assertThat(summary.portionsCount()).isEqualTo(recipe.getPortionsCount());
        assertThat(summary.instructions()).isEqualTo(recipe.getInstructions());

        assertThat(summary.tags()).hasSize(2);
        assertThat(summary.tags()).anyMatch(s -> s.id().equals(tag1.getId()) && s.name().equals("tag 1"));
        assertThat(summary.tags()).anyMatch(s -> s.id().equals(tag2.getId()) && s.name().equals("tag 2"));

        assertThat(summary.ingredients()).hasSize(2);
        assertThat(summary.ingredients()).anyMatch(s -> s.id().equals(ingredient1.getId()) && s.name().equals("ingredient 1") && s.quantity() == 10f && s.unit() == IngredientUnit.PINCH && !s.quantityFixed());
        assertThat(summary.ingredients()).anyMatch(s -> s.id().equals(ingredient2.getId()) && s.name().equals("ingredient 2") && s.quantity() == 5f && s.unit() == IngredientUnit.GRAM && s.quantityFixed());
    }

    @Test
    void handlesMultipleTimesTheSameIngredient() {
        GetRecipeHandler handler = new GetRecipeHandler(recipeService);
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.GRAM)._1;
        Repositories.ingredients().add(ingredient);
        Recipe recipe = Recipe.factory().create("recette", null, 1, Set.of(new RecipeIngredient(ingredient.getId(), 10f, IngredientUnit.GRAM, false), new RecipeIngredient(ingredient.getId(), 5f, IngredientUnit.GRAM, true)), "instructions", Collections.emptySet())._1;
        Repositories.recipes().add(recipe);

        RecipeSummary summary = handler.execute(new GetRecipe(recipe.getId().toString()), template);

        assertThat(summary.ingredients()).hasSize(2);
        assertThat(summary.ingredients()).anyMatch(s -> s.id().equals(ingredient.getId()) && s.name().equals("ingredient") && s.quantity() == 10f && s.unit() == IngredientUnit.GRAM && !s.quantityFixed());
        assertThat(summary.ingredients()).anyMatch(s -> s.id().equals(ingredient.getId()) && s.name().equals("ingredient") && s.quantity() == 5f && s.unit() == IngredientUnit.GRAM && s.quantityFixed());
    }

    @Test
    void throwsAnExceptionWhenTheProvidedIdDoesNotMatchAnyEntity() {
        GetRecipeHandler handler = new GetRecipeHandler(recipeService);
        String stringUuid = UUID.randomUUID().toString();

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(new GetRecipe(stringUuid), template))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + stringUuid);
    }

    @Test
    void canReturnARecipeThatHasNoIngredientsOrTags() {
        GetRecipeHandler handler = new GetRecipeHandler(recipeService);
        Recipe recipe = Recipe.factory().create("recette 1", null, 1, Collections.emptySet(), "instructions", Collections.emptySet())._1;
        Repositories.recipes().add(recipe);

        RecipeSummary summary = handler.execute(new GetRecipe(recipe.getId().toString()), template);

        assertThat(summary.ingredients()).hasSize(0);
        assertThat(summary.tags()).hasSize(0);
    }

    private static class RecipeFactory {
        public static Recipe createEmpty(String name) {
            return Recipe.factory().create(name, null, 1, Collections.emptySet(), "instructions", Collections.emptySet())._1;
        }
    }
}
