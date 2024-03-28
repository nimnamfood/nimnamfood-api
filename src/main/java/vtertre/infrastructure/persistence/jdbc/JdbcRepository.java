package vtertre.infrastructure.persistence.jdbc;

import com.google.common.collect.Streams;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.repository.CrudRepository;
import vtertre.ddd.AggregateRoot;
import vtertre.ddd.Repository;
import vtertre.infrastructure.persistence.DboProvider;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JdbcRepository<TId, TAggregateRoot extends AggregateRoot<TId>, TDbo extends JdbcDbo<TId, TAggregateRoot>> implements Repository<TId, TAggregateRoot>, DboProvider<TId, TAggregateRoot, TDbo> {
    protected final CrudRepository<TDbo, TId> jdbcCrudRepository;
    protected JdbcAggregateTemplate jdbcAggregateTemplate;

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
    public Set<TAggregateRoot> getAll(Predicate<TAggregateRoot> predicate, int limit, int skip) {
        // TODO : Temporaire le temps de retravailler les handlers.
        final Stream<TAggregateRoot> stream = Streams.stream(this.jdbcCrudRepository.findAll()).unordered()
                .map(JdbcDbo::asAggregateRoot)
                .filter(predicate)
                .skip(skip);
        return limit > 0 ? stream.limit(limit).collect(Collectors.toSet()) : stream.collect(Collectors.toSet());
    }
}
