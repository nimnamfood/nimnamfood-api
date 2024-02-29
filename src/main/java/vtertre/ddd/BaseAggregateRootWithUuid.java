package vtertre.ddd;

import java.util.UUID;

public class BaseAggregateRootWithUuid extends BaseAggregateRoot<UUID> implements AggregateRootWithUuid {
    protected BaseAggregateRootWithUuid(UUID uuid) {
        super(uuid);
    }

    protected BaseAggregateRootWithUuid() {
        super(UUID.randomUUID());
    }
}
