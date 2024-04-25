package nimnamfood.query.recipe.projection;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientChanged;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.recipe.RecipesViewTestHelper;
import nimnamfood.query.recipe.model.RecipeIngredientSummaryPartInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RecipesViewTestHelper.class)
class OnIngredientChangedUpdateRecipeViewPartTest extends PostgresTestContainerBase {
    @Autowired
    RecipesViewTestHelper view;
    @Autowired
    JdbcClient client;

    @Test
    void updatesTheSummaryOfTheChangedIngredient() {
        Ingredient i = Ingredient.factory().create("pomme", IngredientUnit.PIECE)._1;
        view.insertIngredients(i);
        IngredientChanged event = new IngredientChanged(i.getId(), "citron", IngredientUnit.GRAM);

        new OnIngredientChangedUpdateRecipeViewPart(client).execute(event);
        RecipeIngredientSummaryPartInspector inspector = view.findIngredient(i.getId()).get();

        assertThat(inspector.id()).isEqualTo(i.getId());
        assertThat(inspector.name()).isEqualTo("citron");
    }
}