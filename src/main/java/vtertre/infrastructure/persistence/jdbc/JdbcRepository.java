package vtertre.infrastructure.persistence.jdbc;

import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.repository.CrudRepository;
import vtertre.ddd.AggregateRoot;
import vtertre.ddd.Repository;
import vtertre.infrastructure.persistence.DboProvider;

import java.util.Optional;

public abstract class JdbcRepository<TId, TAggregateRoot extends AggregateRoot<TId>, TDbo extends JdbcDbo<TId, TAggregateRoot>> implements Repository<TId, TAggregateRoot>, DboProvider<TId, TAggregateRoot, TDbo> {
    protected final CrudRepository<TDbo, TId> jdbcCrudRepository;
    protected final JdbcAggregateTemplate jdbcAggregateTemplate;

    protected JdbcRepository(CrudRepository<TDbo, TId> jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcCrudRepository = jdbcCrudRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public Optional<TAggregateRoot> get(TId tId) {
        return this.jdbcCrudRepository.findById(tId).map(JdbcDbo::asAggregateRoot);
    }

    @Override
    public void add(TAggregateRoot aggregateRoot) {
        final TDbo dbo = this.toDbo(aggregateRoot);
        this.jdbcAggregateTemplate.insert(dbo);
    }

    @Override
    public void update(TAggregateRoot aggregateRoot) {
        final TDbo dbo = this.toDbo(aggregateRoot);
        this.jdbcCrudRepository.save(dbo);
    }

    @Override
    public boolean exists(TId tId) {
        return this.jdbcCrudRepository.existsById(tId);
    }
}
