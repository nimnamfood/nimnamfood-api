package vtertre.infrastructure.persistence.memory;

import com.google.common.collect.Sets;
import vtertre.ddd.AggregateRoot;
import vtertre.ddd.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
    public Set<TAggregateRoot> getAll() {
        return this.entities;
    }
}
