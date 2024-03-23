package vtertre.query;

import vtertre.infrastructure.bus.Message;

public abstract class Query<TResponse> implements Message<TResponse> {
    protected int limit;
    protected int skip;

    public Query<TResponse> limit(int limit) {
        this.limit = limit;
        return this;
    }

    public Query<TResponse> skip(int skip) {
        this.skip = skip;
        return this;
    }

    public int limit() {
        return this.limit;
    }

    public int skip() {
        return this.skip;
    }
}
