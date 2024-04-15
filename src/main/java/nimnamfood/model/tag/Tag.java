package nimnamfood.model.tag;

import vtertre.ddd.BaseAggregateRootWithUuid;

import java.util.UUID;

public class Tag extends BaseAggregateRootWithUuid {
    private final String name;

    public Tag(String name) {
        this.name = name;
    }

    public Tag(UUID id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
