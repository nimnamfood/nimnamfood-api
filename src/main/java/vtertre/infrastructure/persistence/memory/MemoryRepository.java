package vtertre.infrastructure.persistence.memory;

import com.google.common.collect.Sets;
import vtertre.ddd.AggregateRoot;
import vtertre.ddd.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MemoryRepository<TId, TAggregateRoot extends AggregateRoot<TId>> implements Repository<TId, TAggregateRoot> {
    final protected HashSet<TAggregateRoot> entities = Sets.newHashSet();

    @Override
    public Optional<TAggregateRoot> get(TId tId) {
        return this.entities.stream().filter(entity -> entity.getId().equals(tId)).findFirst();
    }

    @Override
    public void add(TAggregateRoot aggregateRoot) {
        this.entities.add(aggregateRoot);
    }

    @Override
    public Set<TAggregateRoot> getAll(Predicate<TAggregateRoot> predicate, int limit, int skip) {
        final Stream<TAggregateRoot> stream = this.entities
                .stream()
                .unordered()
                .filter(predicate)
                .skip(skip);
        return limit > 0 ? stream.limit(limit).collect(Collectors.toSet()) : stream.collect(Collectors.toSet());
    }
}
