package nimnamfood.query;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.recipe.FindRecipes;
import nimnamfood.query.recipe.FindRecipesHandler;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class FindRecipesHandlerTest {
    @Test
    void returnsAnEmptyListOfRecipes() {
        FindRecipesHandler handler = new FindRecipesHandler();

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes());

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllIngredientsWhenNoQueryIsProvided() {
        FindRecipesHandler handler = new FindRecipesHandler();
        Recipe recipe1 = Recipe.factory().create("recette", 1, Collections.emptyList(), "", Collections.emptyList());
        Recipe recipe2 = Recipe.factory().create("recette 2", 1, Collections.emptyList(), "", List.of(new Tag("tag")));
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes());

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe1.getId()) &&
                summary.name.equals(recipe1.getName()) &&
                summary.tags.isEmpty());
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe2.getId()) &&
                summary.name.equals(recipe2.getName()) &&
                summary.tags.getFirst().name.equals("tag"));
    }

    @Test
    void returnsAllIngredientsContainingTheQuery() {
        FindRecipesHandler handler = new FindRecipesHandler();

        Recipe recipe1 = Recipe.factory().create("poulet citron", 1, Collections.emptyList(), "", Collections.emptyList());
        Recipe recipe2 = Recipe.factory().create("crevettes", 1, Collections.emptyList(), "", Collections.emptyList());
        Recipe recipe3 = Recipe.factory().create("riz au poulet", 1, Collections.emptyList(), "", Collections.emptyList());
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("poulet"));

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe1.getId()));
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe3.getId()));
    }

    @Test
    void ignoresTheQueryCaseAndSpecialCharacters() {
        FindRecipesHandler handler = new FindRecipesHandler();
        Repositories.recipes().add(Recipe.factory().create(
                "taboulé de poulet", 1, Collections.emptyList(), "", Collections.emptyList()));

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("taboule"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name).isEqualTo("taboulé de poulet");
    }
}
