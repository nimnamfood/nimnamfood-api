package nimnamfood.query.recipe.projection;

import nimnamfood.model.ingredient.IngredientChanged;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnIngredientChangedUpdateRecipeSummary implements EventCaptor<IngredientChanged> {
    private final JdbcClient client;

    public OnIngredientChangedUpdateRecipeSummary(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(IngredientChanged event) {
        final String sqlQuery = """
                UPDATE view_recipes
                SET ingredients = jsonb_set(ingredients, ('{' || ingredient_index || ',name}')::text[], to_json(:ingredientName)::jsonb)
                FROM (
                	SELECT id, (index - 1) as ingredient_index
                	FROM view_recipes, jsonb_array_elements(ingredients) WITH ORDINALITY arr(item, index)
                	WHERE item ->> 'id' = :ingredientId
                ) AS subquery
                WHERE view_recipes.id = subquery.id;
                """;

        this.client.sql(sqlQuery)
                .param("ingredientId", event.id().toString())
                .param("ingredientName", event.name())
                .update();
    }
}
