package nimnamfood.query.ingredient.projection;

import nimnamfood.model.ingredient.IngredientCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnIngredientCreatedFillSummary implements EventCaptor<IngredientCreated> {
    private final JdbcClient client;

    @Autowired
    public OnIngredientCreatedFillSummary(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(IngredientCreated event) {
        this.client.sql("INSERT INTO view_ingredients VALUES(:id, :name, :unit)")
                .param("id", event.id())
                .param("name", event.name())
                .param("unit", event.unit().toString())
                .update();
    }
}
