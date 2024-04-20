package nimnamfood.query.recipe.projection;

import com.google.common.collect.ImmutableSet;
import nimnamfood.model.recipe.RecipeCreated;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RecipeSearchViewTestHelper.class)
class OnRecipeCreatedFillSearchSummaryTest extends PostgresTestContainerBase {
    @Autowired
    RecipeSearchViewTestHelper searchView;

    @Autowired
    JdbcClient client;

    RecipeService recipeService = Mockito.mock();

    @Test
    void insertsTheSearchSummary() {
        Tag tag1 = Tag.factory().create("1")._1;
        Tag tag2 = Tag.factory().create("2")._1;
        Tag tag3 = Tag.factory().create("3")._1;
        searchView.insertTags(tag2, tag1, tag3);
        RecipeCreated event = new RecipeCreated(UUID.randomUUID(), "recette", UUID.randomUUID(), 1,
                "", ImmutableSet.of(), ImmutableSet.of(tag2.getId(), tag1.getId()), LocalDateTime.now());
        Mockito.when(recipeService.illustrationUrl(event.illustrationId())).thenReturn("url");

        new OnRecipeCreatedFillSearchSummary(client, recipeService).execute(event);
        RecipeSearchSummaryInspector inspector = searchView.findRecipe(event.id());

        assertThat(inspector.id()).isEqualTo(event.id());
        assertThat(inspector.name()).isEqualTo("recette");
        assertThat(inspector.illustrationUrl()).isEqualTo("url");
        assertThat(inspector.creationDateTime()).isEqualTo(event.creationDateTime());
        assertThat(inspector.tags()).hasSize(2);
        assertThat(inspector.tags())
                .anySatisfy(t -> {
                    assertThat(t.id()).isEqualTo(tag1.getId());
                    assertThat(t.name()).isEqualTo(tag1.getName());
                }).anySatisfy(t -> {
                    assertThat(t.id()).isEqualTo(tag2.getId());
                    assertThat(t.name()).isEqualTo(tag2.getName());
                });
    }

    @Test
    void tagsCanBeEmpty() {
        RecipeCreated event = new RecipeCreated(UUID.randomUUID(), "recette", UUID.randomUUID(), 1,
                "", ImmutableSet.of(), ImmutableSet.of(), LocalDateTime.now());

        new OnRecipeCreatedFillSearchSummary(client, recipeService).execute(event);
        RecipeSearchSummaryInspector inspector = searchView.findRecipe(event.id());

        assertThat(inspector.tags()).hasSize(0);
    }

    @Test
    void illustrationUrlCanBeNull() {
        RecipeCreated event = new RecipeCreated(UUID.randomUUID(), "", null, 1,
                "", ImmutableSet.of(), ImmutableSet.of(), LocalDateTime.now());

        new OnRecipeCreatedFillSearchSummary(client, recipeService).execute(event);
        RecipeSearchSummaryInspector inspector = searchView.findRecipe(event.id());

        assertThat(inspector.illustrationUrl()).isNull();
    }
}