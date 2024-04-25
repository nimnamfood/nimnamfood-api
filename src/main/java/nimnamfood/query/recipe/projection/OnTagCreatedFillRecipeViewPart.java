package nimnamfood.query.recipe.projection;

import nimnamfood.model.tag.TagCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnTagCreatedFillRecipeViewPart implements EventCaptor<TagCreated> {
    private final JdbcClient client;

    @Autowired
    public OnTagCreatedFillRecipeViewPart(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(TagCreated event) {
        this.client.sql("INSERT INTO view_part_recipe_tags (id, name) VALUES (:id, :name)")
                .param("id", event.id())
                .param("name", event.name())
                .update();
    }
}
