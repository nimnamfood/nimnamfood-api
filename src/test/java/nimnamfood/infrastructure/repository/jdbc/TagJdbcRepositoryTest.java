package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.jdbc.tag.TagDbo;
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcRepository;
import nimnamfood.model.tag.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

public class TagJdbcRepositoryTest extends PostgresTestContainerBase {
    @Autowired
    TagJdbcCrudRepository crudRepository;
    @Autowired
    JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void retrievesATag() {
        TagJdbcRepository repository = new TagJdbcRepository(crudRepository, jdbcAggregateTemplate);
        TagDbo dbo = new TagDbo();
        dbo.setId(UUID.randomUUID());
        dbo.setName("rapide");
        this.jdbcAggregateTemplate.insert(dbo);

        Optional<Tag> foundTag = repository.get(dbo.getId());

        assertThat(foundTag).isPresent();
        assertThat(foundTag.get().getId()).isEqualTo(dbo.getId());
        assertThat(foundTag.get().getName()).isEqualTo("rapide");
    }

    @Test
    void addsATag() {
        TagJdbcRepository repository = new TagJdbcRepository(crudRepository, jdbcAggregateTemplate);
        Tag tag = Tag.factory().create("végé")._1;

        repository.add(tag);
        TagDbo dbo = this.jdbcAggregateTemplate.findById(tag.getId(), TagDbo.class);

        assertThat(dbo).isNotNull();
        assertThat(dbo.getName()).isEqualTo("végé");
    }

    @Test
    void throwsAnErrorWhenAddingATagWithAnExistingName() {
        TagJdbcRepository repository = new TagJdbcRepository(crudRepository, jdbcAggregateTemplate);

        repository.add(Tag.factory().create("rapide")._1);

        assertThatRuntimeException()
                .isThrownBy(() -> repository.add(Tag.factory().create("rapide")._1))
                .withCauseInstanceOf(DuplicateKeyException.class);
    }
}
