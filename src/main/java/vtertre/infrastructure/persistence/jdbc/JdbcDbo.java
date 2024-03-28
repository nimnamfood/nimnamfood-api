package vtertre.infrastructure.persistence.jdbc;

import vtertre.ddd.AggregateRoot;

public interface JdbcDbo<TId, TAggregateRoot extends AggregateRoot<TId>> {
    TAggregateRoot asAggregateRoot();
}
