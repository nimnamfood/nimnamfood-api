package vtertre.infrastructure.persistence.memory;

import com.google.common.collect.Sets;
import vtertre.ddd.Repository;

import java.util.Set;

public class MemoryRepository<T> implements Repository<T> {
    final Set<T> entities = Sets.newHashSet();

    @Override
    public void add(T entity) {
        this.entities.add(entity);
    }

    public Set<T> getAll() {
        return this.entities;
    }
}
