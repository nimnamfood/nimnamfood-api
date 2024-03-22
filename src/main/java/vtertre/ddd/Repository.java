package vtertre.ddd;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface Repository<TId, TAggregateRoot extends AggregateRoot<TId>> {
    Optional<TAggregateRoot> get(TId id);

    void add(TAggregateRoot aggregateRoot);

    Set<TAggregateRoot> getAll(Predicate<TAggregateRoot> predicate);
}
