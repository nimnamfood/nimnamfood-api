package nimnamfood.query;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.model.TagSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class FindAllTagsHandlerTest {

    @Test
    void returnsAnEmptyListOfTags() {
        FindAllTagsHandler handler = new FindAllTagsHandler();

        List<TagSummary> result = handler.execute(new FindAllTags());

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsANonEmptyListOfTags() {
        FindAllTagsHandler handler = new FindAllTagsHandler();
        Tag tag = new Tag("rapide");
        Repositories.tags().add(tag);

        List<TagSummary> result = handler.execute(new FindAllTags());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id).isEqualTo(tag.getId());
        assertThat(result.getFirst().name).isEqualTo("rapide");
    }
}
