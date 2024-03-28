package vtertre.infrastructure.persistence.jdbc;

import vtertre.ddd.AggregateRoot;

public class FakeAggregateRoot implements AggregateRoot<String> {
    String id;
    String name;

    public FakeAggregateRoot(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}
