package nimnamfood.model.tag;

import nimnamfood.infrastructure.repository.jdbc.tag.TagDbo;
import vtertre.ddd.BaseAggregateRootWithUuid;
import vtertre.ddd.Tuple;

import java.util.UUID;

public class Tag extends BaseAggregateRootWithUuid {
    private final String name;

    public static Factory factory() {
        return new Factory();
    }

    private Tag(String name) {
        this.name = name;
    }

    private Tag(UUID id, String name) {
        super(id);
        this.name = name;
    }

    public static class Factory {
        public Tuple<Tag, TagCreated> create(String name) {
            final Tag tag = new Tag(name);
            return Tuple.of(tag, new TagCreated(tag.getId(), tag.name));
        }

        public Tag recreateFromDbo(TagDbo dbo) {
            return new Tag(dbo.getId(), dbo.getName());
        }
    }

    public String getName() {
        return this.name;
    }
}
