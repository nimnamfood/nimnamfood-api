package nimnamfood.infrastructure.repository.jdbc.tag;

import nimnamfood.model.tag.Tag;
import org.springframework.data.relational.core.mapping.Table;
import vtertre.infrastructure.persistence.jdbc.BaseJdbcDboWithUuid;

@Table("tags")
public class TagDbo extends BaseJdbcDboWithUuid<Tag> {
    String name;

    @Override
    public Tag asAggregateRoot() {
        final Tag tag = new Tag(this.name);
        tag.setId(this.getId());
        return tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
