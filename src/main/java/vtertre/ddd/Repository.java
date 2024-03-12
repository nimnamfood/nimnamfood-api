package vtertre.ddd;

import java.util.Optional;
import java.util.Set;

public interface Repository<TId, TAggregateRoot extends AggregateRoot<TId>> {
    Optional<TAggregateRoot> get(TId id);

    void add(TAggregateRoot aggregateRoot);

    Set<TAggregateRoot> getAll();
}
