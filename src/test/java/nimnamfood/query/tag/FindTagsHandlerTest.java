package nimnamfood.query.tag;

import nimnamfood.infrastructure.repository.jdbc.WithJdbcRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.tag.model.TagSummary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(WithJdbcRepositories.class)
public class FindTagsHandlerTest extends PostgresTestContainerBase {
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
        Tag tag = new Tag("rapide");
        Repositories.tags().add(tag);

        List<TagSummary> result = handler.execute(new FindTags(), template);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(tag.getId());
        assertThat(result.getFirst().name()).isEqualTo("rapide");
    }

    @Test
    void returnsAllTagsContainingTheQuery() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag1 = new Tag("rapide");
        Tag tag2 = new Tag("végé");
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);

        List<TagSummary> result = handler.execute(new FindTags("vég"), template);

        assertThat(result).hasSize(1);
        assertThat(result).anyMatch(summary -> summary.id().equals(tag2.getId()) &&
                summary.name().equals(tag2.getName()));
    }

    @Disabled("Désactivé le temps de trouver comment ignorer les caractères spéciaux côté DB ou via les projections")
    @Test
    void ignoresTheQueryCaseAndSpecialCharacters() {
        FindTagsHandler handler = new FindTagsHandler();
        Tag tag1 = new Tag("rapide");
        Tag tag2 = new Tag("végé");
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);

        List<TagSummary> result = handler.execute(new FindTags("veg"), template);

        assertThat(result).hasSize(1);
        assertThat(result).anyMatch(summary -> summary.id().equals(tag2.getId()) &&
                summary.name().equals(tag2.getName()));
    }
}
