package nimnamfood.query.tag.projection;

import nimnamfood.model.tag.TagCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnTagCreatedFillSummary implements EventCaptor<TagCreated> {
    final JdbcClient client;

    @Autowired
    public OnTagCreatedFillSummary(JdbcClient client) {
        this.client = client;
    }

    @Override
    public void execute(TagCreated event) {
        this.client.sql("INSERT INTO view_tags VALUES (:id, :name)")
                .param("id", event.id())
                .param("name", event.name())
                .update();
    }
}
