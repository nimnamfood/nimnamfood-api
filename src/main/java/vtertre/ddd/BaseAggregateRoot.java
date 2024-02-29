package vtertre.ddd;

public abstract class BaseAggregateRoot<TId> extends BaseEntity<TId> implements AggregateRoot<TId> {
    protected BaseAggregateRoot(TId tId) {
        super(tId);
    }
}
