package nimnamfood.query;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
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
        Repositories.tags().add("Rapide");
        Repositories.tags().add("Végé");

        List<TagSummary> result = handler.execute(new FindAllTags());

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().name).isEqualTo("Rapide");
        assertThat(result.getLast().name).isEqualTo("Végé");
    }
}
