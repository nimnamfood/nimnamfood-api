package vtertre.infrastructure.persistence;

import vtertre.ddd.AggregateRoot;
import vtertre.infrastructure.persistence.jdbc.JdbcDbo;

public interface DboProvider<TId, TAggregateRoot extends AggregateRoot<TId>, TDbo extends JdbcDbo<TId, TAggregateRoot>> {
    TDbo toDbo(TAggregateRoot aggregateRoot);
}
