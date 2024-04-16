package nimnamfood.query.ingredient.projection;

import nimnamfood.model.ingredient.IngredientChanged;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnIngredientChangedUpdateSummary implements EventCaptor<IngredientChanged> {
    private final JdbcClient client;

    @Autowired
    public OnIngredientChangedUpdateSummary(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(IngredientChanged event) {
        this.client.sql("UPDATE view_ingredients SET name = :name, unit = :unit WHERE id = :id")
                .param("id", event.id())
                .param("name", event.name())
                .param("unit", event.unit().toString())
                .update();
    }
}
