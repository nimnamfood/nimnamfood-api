package vtertre.infrastructure.persistence.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FakeAggregateRootJdbcRepository extends JdbcRepository<String, FakeAggregateRoot, FakeAggregateRootDbo> {
    @Autowired
    public FakeAggregateRootJdbcRepository(FakeAggregateRootJdbcCrudRepository jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        super(jdbcCrudRepository, jdbcAggregateTemplate);
    }

    @Override
    public FakeAggregateRootDbo toDbo(FakeAggregateRoot aggregateRoot) {
        final FakeAggregateRootDbo dbo = new FakeAggregateRootDbo();
        dbo.id = aggregateRoot.id;
        dbo.name = aggregateRoot.name;
        return dbo;
    }
}
