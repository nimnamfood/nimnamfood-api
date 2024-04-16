package nimnamfood.query.tag.projection;

import nimnamfood.model.tag.TagCreated;
import nimnamfood.query.tag.TagsViewTestHelper;
import nimnamfood.query.tag.model.TagSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TagsViewTestHelper.class)
class OnTagCreatedFillSummaryTest extends PostgresTestContainerBase {
    @Autowired
    TagsViewTestHelper view;

    @Autowired
    JdbcClient client;

    @Test
    void insertsTheSummaryOfTheTagIntoTheView() {
        TagCreated event = new TagCreated(UUID.randomUUID(), "tag");

        new OnTagCreatedFillSummary(client).execute(event);
        TagSummary result = view.findTag(event.id()).get();

        assertThat(result.id()).isEqualTo(event.id());
        assertThat(result.name()).isEqualTo("tag");
    }
}