package vtertre.ddd;

import java.util.Set;

public interface Repository<TId, TAggregateRoot extends AggregateRoot<TId>> {
    void add(TAggregateRoot aggregateRoot);

    Set<TAggregateRoot> getAll();
}
