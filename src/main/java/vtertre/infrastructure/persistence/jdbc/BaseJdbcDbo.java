package vtertre.infrastructure.persistence.jdbc;

import org.springframework.data.annotation.Id;
import vtertre.ddd.AggregateRoot;

public abstract class BaseJdbcDbo<TId, TAggregateRoot extends AggregateRoot<TId>> implements JdbcDbo<TId, TAggregateRoot> {
    @Id
    TId id;

    public TId getId() {
        return id;
    }

    public void setId(TId id) {
        this.id = id;
    }
}
