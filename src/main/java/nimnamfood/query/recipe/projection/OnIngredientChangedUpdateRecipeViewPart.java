package nimnamfood.query.recipe.projection;

import nimnamfood.model.ingredient.IngredientChanged;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnIngredientChangedUpdateRecipeViewPart implements EventCaptor<IngredientChanged> {
    private final JdbcClient client;

    @Autowired
    public OnIngredientChangedUpdateRecipeViewPart(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(IngredientChanged event) {
        this.client.sql("UPDATE view_part_recipe_ingredients SET name = :name WHERE id = :id")
                .param("id", event.id())
                .param("name", event.name())
                .update();
    }
}
