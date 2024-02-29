package nimnamfood.model.tag;

import vtertre.ddd.BaseAggregateRootWithUuid;

public class Tag extends BaseAggregateRootWithUuid {
    private final String name;

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
