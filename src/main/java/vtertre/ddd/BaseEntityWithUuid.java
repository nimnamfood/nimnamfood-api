package vtertre.ddd;

import java.util.UUID;

public abstract class BaseEntityWithUuid extends BaseEntity<UUID> implements EntityWithUuid {
    protected BaseEntityWithUuid() {
        super(UUID.randomUUID());
    }

    protected BaseEntityWithUuid(UUID uuid) {
        super(uuid);
    }
}
