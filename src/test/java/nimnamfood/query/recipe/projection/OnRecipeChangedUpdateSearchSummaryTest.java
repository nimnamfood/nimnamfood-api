package nimnamfood.query.recipe.projection;

import com.google.common.collect.ImmutableSet;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeChanged;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.recipe.RecipeSearchViewTestHelper;
import nimnamfood.query.recipe.model.RecipeSearchSummaryInspector;
import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Import(RecipeSearchViewTestHelper.class)
class OnRecipeChangedUpdateSearchSummaryTest extends PostgresTestContainerBase {
    @Autowired
    RecipeSearchViewTestHelper searchView;

    @Autowired
    JdbcClient client;

    RecipeService recipeService = Mockito.mock();

    @Test
    void updatesTheSearchSummaryWithoutTags() {
        Recipe recipe = Recipe.factory().create("recette", 1, Collections.emptySet(), "", Collections.emptySet())._1;
        searchView.insertRecipes(recipe);
        RecipeChanged event = new RecipeChanged(recipe.getId(), "recette updated", UUID.randomUUID(), 1, "", ImmutableSet.of(), ImmutableSet.of());
        Mockito.when(recipeService.illustrationUrl(event.illustrationId())).thenReturn("url");

        new OnRecipeChangedUpdateSearchSummary(client, recipeService).execute(event);
        RecipeSearchSummaryInspector inspector = searchView.findRecipe(recipe.getId());

        assertThat(inspector.name()).isEqualTo("recette updated");
        assertThat(inspector.illustrationUrl()).isEqualTo("url");
        assertThat(inspector.creationDateTime()).isCloseTo(recipe.getCreationDateTime(), within(1, ChronoUnit.MICROS));
        assertThat(inspector.tags()).isEmpty();
    }

    @Test
    void updatesTheSearchSummary() {
        Tag tag = Tag.factory().create("tag")._1;
        searchView.insertTags(tag);
        Recipe recipe = Recipe.factory().create("recette", 1, Collections.emptySet(), "", Collections.emptySet())._1;
        searchView.insertRecipes(recipe);
        RecipeChanged event = new RecipeChanged(recipe.getId(), "recette updated", UUID.randomUUID(), 1, "", ImmutableSet.of(), ImmutableSet.of(tag.getId()));
        Mockito.when(recipeService.illustrationUrl(event.illustrationId())).thenReturn("url");

        new OnRecipeChangedUpdateSearchSummary(client, recipeService).execute(event);
        RecipeSearchSummaryInspector inspector = searchView.findRecipe(recipe.getId());

        assertThat(inspector.name()).isEqualTo("recette updated");
        assertThat(inspector.illustrationUrl()).isEqualTo("url");
        assertThat(inspector.creationDateTime()).isCloseTo(recipe.getCreationDateTime(), within(1, ChronoUnit.MICROS));
        assertThat(inspector.tags()).hasSize(1)
                .first()
                .satisfies(t -> {
                    assertThat(t.id()).isEqualTo(tag.getId());
                    assertThat(t.name()).isEqualTo(tag.getName());
                });
    }
}