package nimnamfood.query;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.tag.FindTags;
import nimnamfood.query.tag.FindTagsHandler;
import nimnamfood.query.tag.model.TagSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class FindTagsHandlerTest {

    @Test
    void returnsAnEmptyListOfTags() {
        FindTagsHandler handler = new FindTagsHandler();

        List<TagSummary> result = handler.execute(new FindTags());

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllTagsWhenNoQueryIsProvided() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag = new Tag("rapide");
        Repositories.tags().add(tag);

        List<TagSummary> result = handler.execute(new FindTags());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id).isEqualTo(tag.getId());
        assertThat(result.getFirst().name).isEqualTo("rapide");
    }

    @Test
    void returnsAllTagsContainingTheQuery() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag1 = new Tag("rapide");
        Tag tag2 = new Tag("végé");
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);

        List<TagSummary> result = handler.execute(new FindTags("vég"));

        assertThat(result).hasSize(1);
        assertThat(result).anyMatch(summary -> summary.id.equals(tag2.getId()) &&
                summary.name.equals(tag2.getName()));
    }

    @Test
    void ignoresTheQueryCaseAndSpecialCharacters() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag1 = new Tag("rapide");
        Tag tag2 = new Tag("végé");
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);

        List<TagSummary> result = handler.execute(new FindTags("veg"));

        assertThat(result).hasSize(1);
        assertThat(result).anyMatch(summary -> summary.id.equals(tag2.getId()) &&
                summary.name.equals(tag2.getName()));
    }
}
