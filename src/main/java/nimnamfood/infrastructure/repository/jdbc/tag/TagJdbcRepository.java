package nimnamfood.infrastructure.repository.jdbc.tag;

import nimnamfood.model.tag.Tag;
import nimnamfood.model.tag.TagRepository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.JdbcRepositoryWithUuid;

public class TagJdbcRepository extends JdbcRepositoryWithUuid<Tag, TagDbo> implements TagRepository {

    public TagJdbcRepository(TagJdbcCrudRepository jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        super(jdbcCrudRepository, jdbcAggregateTemplate);
    }

    @Override
    public TagDbo toDbo(Tag tag) {
        final TagDbo dbo = new TagDbo();
        dbo.setId(tag.getId());
        dbo.name = tag.getName();
        return dbo;
    }
}
