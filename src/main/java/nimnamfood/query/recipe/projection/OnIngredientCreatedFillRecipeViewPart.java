package nimnamfood.query.recipe.projection;

import nimnamfood.model.ingredient.IngredientCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnIngredientCreatedFillRecipeViewPart implements EventCaptor<IngredientCreated> {
    private final JdbcClient client;

    @Autowired
    public OnIngredientCreatedFillRecipeViewPart(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(IngredientCreated event) {
        this.client.sql("INSERT INTO view_part_recipe_ingredients (id, name) VALUES(:id, :name)")
                .param("id", event.id())
                .param("name", event.name())
                .update();
    }
}
