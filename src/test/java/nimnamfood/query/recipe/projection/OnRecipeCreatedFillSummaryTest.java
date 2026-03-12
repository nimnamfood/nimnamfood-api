package nimnamfood.query.recipe.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.RecipeCreated;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.ObjectMapperFactory;
import nimnamfood.query.recipe.RecipesViewTestHelper;
import nimnamfood.query.recipe.model.RecipeSummaryInspector;
import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RecipesViewTestHelper.class)
class OnRecipeCreatedFillSummaryTest extends PostgresTestContainerBase {
    @Autowired
    RecipesViewTestHelper view;
    @Autowired
    JdbcClient client;

    RecipeService recipeService = Mockito.mock();
    ObjectMapper mapper = ObjectMapperFactory.withSnakeCasePropertyNamingStrategy();
    OnRecipeCreatedFillSummary handler;

    @BeforeEach
    void setUp() {
        handler = new OnRecipeCreatedFillSummary();
        handler.setClient(client);
        handler.setRecipeService(recipeService);
        handler.setMapper(mapper);
    }

    @Test
    void insertsTheRecipeSummary() {
        Tag tag = Tag.factory().create("tag")._1;
        view.insertTags(tag);
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.PIECE)._1;
        view.insertIngredients(ingredient);
        RecipeIngredient ri = new RecipeIngredient(ingredient.getId(), 10.5f, IngredientUnit.GRAM);
        RecipeCreated event = new RecipeCreated(UUID.randomUUID(), "recette", UUID.randomUUID(), 1,
                "instructions", ImmutableSet.of(ri), ImmutableSet.of(tag.getId()), Instant.now());
        Mockito.when(recipeService.illustrationUrl(event.illustrationId())).thenReturn("url");

        handler.execute(event);
        RecipeSummaryInspector inspector = view.findRecipe(event.id());

        assertThat(inspector.id()).isEqualTo(event.id());
        assertThat(inspector.name()).isEqualTo("recette");
        assertThat(inspector.illustration().id()).isEqualTo(event.illustrationId());
        assertThat(inspector.illustration().url()).isEqualTo("url");
        assertThat(inspector.portionsCount()).isEqualTo(1);
        assertThat(inspector.instructions()).isEqualTo("instructions");
        assertThat(inspector.ingredients()).hasSize(1).first().satisfies(i -> {
            assertThat(i.id()).isEqualTo(ingredient.getId());
            assertThat(i.name()).isEqualTo("ingredient");
            assertThat(i.quantity()).isEqualTo(10.5f);
            assertThat(i.unit()).isEqualTo(IngredientUnit.GRAM);
        });
        assertThat(inspector.tags()).hasSize(1).first().satisfies(t -> {
            assertThat(t.id()).isEqualTo(tag.getId());
            assertThat(t.name()).isEqualTo("tag");
        });
    }

    @Test
    void ingredientsCanBeEmpty() {
        RecipeCreated event = new RecipeCreated(UUID.randomUUID(), "recette", UUID.randomUUID(), 1,
                "instructions", ImmutableSet.of(), ImmutableSet.of(), Instant.now());
        Mockito.when(recipeService.illustrationUrl(event.illustrationId())).thenReturn("url");

        handler.execute(event);
        RecipeSummaryInspector inspector = view.findRecipe(event.id());

        assertThat(inspector.ingredients()).isEmpty();
    }

    @Test
    void tagsCanBeEmpty() {
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.PIECE)._1;
        view.insertIngredients(ingredient);
        RecipeIngredient ri = new RecipeIngredient(ingredient.getId(), 10.5f, IngredientUnit.GRAM);
        RecipeCreated event = new RecipeCreated(UUID.randomUUID(), "recette", UUID.randomUUID(), 1,
                "instructions", ImmutableSet.of(ri), ImmutableSet.of(), Instant.now());
        Mockito.when(recipeService.illustrationUrl(event.illustrationId())).thenReturn("url");

        handler.execute(event);
        RecipeSummaryInspector inspector = view.findRecipe(event.id());

        assertThat(inspector.tags()).isEmpty();
    }

    @Test
    void illustrationCanBeEmpty() {
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.PIECE)._1;
        view.insertIngredients(ingredient);
        RecipeIngredient ri = new RecipeIngredient(ingredient.getId(), 10.5f, IngredientUnit.GRAM);
        RecipeCreated event = new RecipeCreated(UUID.randomUUID(), "recette", null, 1,
                "instructions", ImmutableSet.of(ri), ImmutableSet.of(), Instant.now());

        handler.execute(event);
        RecipeSummaryInspector inspector = view.findRecipe(event.id());

        assertThat(inspector.illustration()).isNull();
    }
}