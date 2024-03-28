package vtertre.infrastructure.persistence.memory;

import vtertre.ddd.AggregateRootWithUuid;
import vtertre.ddd.RepositoryWithUuid;

import java.util.UUID;

public abstract class MemoryRepositoryWithUuid<TAggregateRoot extends AggregateRootWithUuid> extends MemoryRepository<UUID, TAggregateRoot> implements RepositoryWithUuid<TAggregateRoot> {
}
