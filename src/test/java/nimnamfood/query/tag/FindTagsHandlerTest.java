package nimnamfood.query.tag;

import nimnamfood.model.tag.Tag;
import nimnamfood.query.tag.model.TagSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TagsViewTestHelper.class)
public class FindTagsHandlerTest extends PostgresTestContainerBase {
    @Autowired
    TagsViewTestHelper view;

    @Autowired
    NamedParameterJdbcTemplate template;

    @Test
    void returnsAnEmptyListOfTags() {
        FindTagsHandler handler = new FindTagsHandler();

        List<TagSummary> result = handler.execute(new FindTags(), template);

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllTagsWhenNoQueryIsProvided() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag = Tag.factory().create("rapide")._1;
        view.insertTags(tag);

        List<TagSummary> result = handler.execute(new FindTags(), template);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(tag.getId());
        assertThat(result.getFirst().name()).isEqualTo("rapide");
    }

    @Test
    void returnsAllTagsContainingTheQuery() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag1 = Tag.factory().create("rapide")._1;
        Tag tag2 = Tag.factory().create("végé")._1;
        view.insertTags(tag1, tag2);

        List<TagSummary> result = handler.execute(new FindTags("vég"), template);

        assertThat(result).hasSize(1);
        assertThat(result).anyMatch(summary -> summary.id().equals(tag2.getId()) &&
                summary.name().equals(tag2.getName()));
    }

    @Test
    void ignoresTheQueryCaseAndSpecialCharacters() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag1 = Tag.factory().create("rapide")._1;
        Tag tag2 = Tag.factory().create("végé")._1;
        view.insertTags(tag1, tag2);

        List<TagSummary> result = handler.execute(new FindTags("veg"), template);

        assertThat(result).hasSize(1);
        assertThat(result).anyMatch(summary -> summary.id().equals(tag2.getId()) &&
                summary.name().equals(tag2.getName()));
    }
}
