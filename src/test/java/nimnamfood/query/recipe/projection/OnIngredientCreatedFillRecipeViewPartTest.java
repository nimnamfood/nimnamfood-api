package nimnamfood.query.recipe.projection;

import nimnamfood.model.ingredient.IngredientCreated;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.recipe.RecipesViewTestHelper;
import nimnamfood.query.recipe.model.RecipeIngredientSummaryPartInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RecipesViewTestHelper.class)
class OnIngredientCreatedFillRecipeViewPartTest extends PostgresTestContainerBase {
    @Autowired
    RecipesViewTestHelper view;
    @Autowired
    JdbcClient client;

    @Test
    void insertsTheIngredientIntoTheView() {
        IngredientCreated event = new IngredientCreated(UUID.randomUUID(), "1", IngredientUnit.GRAM);

        new OnIngredientCreatedFillRecipeViewPart(client).execute(event);
        RecipeIngredientSummaryPartInspector result = view.findIngredient(event.id()).get();

        assertThat(result.id()).isEqualTo(event.id());
        assertThat(result.name()).isEqualTo("1");
    }
}