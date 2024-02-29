package vtertre.ddd;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public abstract class BaseEntity<TId> implements Entity<TId> {
    private TId id;

    protected BaseEntity(TId id) {
        this.id = id;
    }

    @Override
    public TId getId() {
        return this.id;
    }

    public void setId(TId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
