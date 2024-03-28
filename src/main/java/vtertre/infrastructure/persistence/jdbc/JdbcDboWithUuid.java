package vtertre.infrastructure.persistence.jdbc;

import vtertre.ddd.AggregateRootWithUuid;

import java.util.UUID;

public interface JdbcDboWithUuid<TAggregateRoot extends AggregateRootWithUuid> extends JdbcDbo<UUID, TAggregateRoot> {
}
