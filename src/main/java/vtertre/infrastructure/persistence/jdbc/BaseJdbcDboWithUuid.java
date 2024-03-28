package vtertre.infrastructure.persistence.jdbc;

import vtertre.ddd.AggregateRootWithUuid;

import java.util.UUID;

public abstract class BaseJdbcDboWithUuid<TAggregateRoot extends AggregateRootWithUuid> extends BaseJdbcDbo<UUID, TAggregateRoot> implements JdbcDboWithUuid<TAggregateRoot> {
}
