package vtertre.ddd;

import java.util.UUID;

public interface RepositoryWithUuid<TAggregateRoot extends AggregateRootWithUuid> extends Repository<UUID, TAggregateRoot> {
}
