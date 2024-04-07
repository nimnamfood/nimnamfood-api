package vtertre.ddd;

import java.util.Optional;

public interface Repository<TId, TAggregateRoot extends AggregateRoot<TId>> {
    Optional<TAggregateRoot> get(TId id);

    void add(TAggregateRoot aggregateRoot);

    void update(TAggregateRoot aggregateRoot);

    boolean exists(TId id);
}
