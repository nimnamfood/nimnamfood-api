package vtertre.ddd;

import java.util.Set;

public interface Repository<T> {
    void add(T entity);

    Set<T> getAll();
}
