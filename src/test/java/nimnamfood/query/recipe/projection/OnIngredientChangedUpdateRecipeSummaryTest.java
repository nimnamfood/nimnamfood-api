package nimnamfood.query.recipe.projection;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientChanged;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.query.recipe.RecipesViewTestHelper;
import nimnamfood.query.recipe.model.RecipeSummaryInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RecipesViewTestHelper.class)
class OnIngredientChangedUpdateRecipeSummaryTest extends PostgresTestContainerBase {
    @Autowired
    RecipesViewTestHelper view;
    @Autowired
    JdbcClient client;

    @Test
    void updatesTheNameOfTheIngredientOnAllRecipes() {
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.PIECE)._1;
        view.insertIngredients(ingredient);
        Recipe recipe1 = Recipe.factory().create("", 1, Set.of(new RecipeIngredient(ingredient.getId(), 1, IngredientUnit.GRAM, false)), "", Collections.emptySet())._1;
        Recipe recipe2 = Recipe.factory().create("", 1, Set.of(new RecipeIngredient(ingredient.getId(), 1, IngredientUnit.GRAM, false)), "", Collections.emptySet())._1;
        view.insertRecipes(Map.of(ingredient.getId(), ingredient.getName()), recipe1, recipe2);

        new OnIngredientChangedUpdateRecipeSummary(client).execute(new IngredientChanged(ingredient.getId(), "ingredient renamed", IngredientUnit.PIECE));
        RecipeSummaryInspector recipe1Inspector = view.findRecipe(recipe1.getId());
        RecipeSummaryInspector recipe2Inspector = view.findRecipe(recipe2.getId());

        assertThat(recipe1Inspector.ingredients()).first().satisfies(i -> {
            assertThat(i.id()).isEqualTo(ingredient.getId());
            assertThat(i.name()).isEqualTo("ingredient renamed");
        });
        assertThat(recipe2Inspector.ingredients()).first().satisfies(i -> {
            assertThat(i.id()).isEqualTo(ingredient.getId());
            assertThat(i.name()).isEqualTo("ingredient renamed");
        });
    }

    @Test
    void preservesUnmodifiedIngredients() {
        Ingredient ingredient1 = Ingredient.factory().create("ingredient 1", IngredientUnit.PIECE)._1;
        Ingredient ingredient2 = Ingredient.factory().create("ingredient 2", IngredientUnit.PIECE)._1;
        view.insertIngredients(ingredient2, ingredient1);
        Recipe recipe = Recipe.factory().create("", 1, Set.of(new RecipeIngredient(ingredient1.getId(), 1, IngredientUnit.GRAM, false), new RecipeIngredient(ingredient2.getId(), 1, IngredientUnit.GRAM, false)), "", Collections.emptySet())._1;
        view.insertRecipes(Map.of(ingredient1.getId(), ingredient1.getName(), ingredient2.getId(), ingredient2.getName()), recipe);

        new OnIngredientChangedUpdateRecipeSummary(client).execute(new IngredientChanged(ingredient1.getId(), "ingredient1 renamed", IngredientUnit.PIECE));
        RecipeSummaryInspector recipeInspector = view.findRecipe(recipe.getId());

        assertThat(recipeInspector.ingredients())
                .filteredOn(i -> i.id().equals(ingredient2.getId()))
                .first()
                .matches(i -> i.name().equals("ingredient 2"));
    }

    @Test
    void preservesOtherProperties() {
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.PIECE)._1;
        view.insertIngredients(ingredient);
        Recipe recipe = Recipe.factory().create("recette", 1, Set.of(new RecipeIngredient(ingredient.getId(), 1, IngredientUnit.GRAM, true)), "instructions", Collections.emptySet())._1;
        view.insertRecipes(Map.of(ingredient.getId(), ingredient.getName()), recipe);

        new OnIngredientChangedUpdateRecipeSummary(client).execute(new IngredientChanged(ingredient.getId(), "ingredient1 renamed", IngredientUnit.TEASPOON));
        RecipeSummaryInspector recipeInspector = view.findRecipe(recipe.getId());

        assertThat(recipeInspector.id()).isEqualTo(recipe.getId());
        assertThat(recipeInspector.name()).isEqualTo("recette");
        assertThat(recipeInspector.illustration()).isNull();
        assertThat(recipeInspector.portionsCount()).isEqualTo(1);
        assertThat(recipeInspector.tags()).isEmpty();
        assertThat(recipeInspector.ingredients()).first().satisfies(i -> {
            assertThat(i.quantity()).isEqualTo(1);
            assertThat(i.unit()).isEqualTo(IngredientUnit.GRAM);
            assertThat(i.quantityFixed()).isTrue();
        });
    }
}