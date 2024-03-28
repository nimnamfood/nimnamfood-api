package vtertre.infrastructure.persistence.jdbc;

import org.springframework.data.relational.core.mapping.Table;

@Table("far")
public class FakeAggregateRootDbo extends BaseJdbcDbo<String, FakeAggregateRoot> {
    String name;

    @Override
    public FakeAggregateRoot asAggregateRoot() {
        return new FakeAggregateRoot(this.id, this.name);
    }
}
