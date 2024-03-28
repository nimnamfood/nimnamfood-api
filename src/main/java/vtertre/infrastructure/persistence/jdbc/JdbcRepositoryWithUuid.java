package vtertre.infrastructure.persistence.jdbc;

import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.repository.CrudRepository;
import vtertre.ddd.AggregateRootWithUuid;
import vtertre.ddd.RepositoryWithUuid;

import java.util.UUID;

public abstract class JdbcRepositoryWithUuid<TAggregateRoot extends AggregateRootWithUuid, TDbo extends JdbcDboWithUuid<TAggregateRoot>> extends JdbcRepository<UUID, TAggregateRoot, TDbo> implements RepositoryWithUuid<TAggregateRoot> {
    protected JdbcRepositoryWithUuid(CrudRepository<TDbo, UUID> jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        super(jdbcCrudRepository, jdbcAggregateTemplate);
    }
}
