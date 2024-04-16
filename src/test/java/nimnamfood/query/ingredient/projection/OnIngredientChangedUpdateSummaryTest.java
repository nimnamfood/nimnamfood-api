package nimnamfood.query.ingredient.projection;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientChanged;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.ingredient.IngredientsViewTestHelper;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import static org.assertj.core.api.Assertions.assertThat;

@Import(IngredientsViewTestHelper.class)
class OnIngredientChangedUpdateSummaryTest extends PostgresTestContainerBase {
    @Autowired
    IngredientsViewTestHelper view;

    @Autowired
    JdbcClient client;

    @Test
    void updatesTheSummaryOfTheChangedIngredient() {
        Ingredient i = Ingredient.factory().create("pomme", IngredientUnit.PIECE)._1;
        view.insertIngredients(i);
        IngredientChanged event = new IngredientChanged(i.getId(), "citron", IngredientUnit.GRAM);

        new OnIngredientChangedUpdateSummary(client).execute(event);
        IngredientSummary summary = view.findIngredient(i.getId()).get();

        assertThat(summary.name()).isEqualTo("citron");
        assertThat(summary.unit()).isEqualTo(IngredientUnit.GRAM);
    }
}