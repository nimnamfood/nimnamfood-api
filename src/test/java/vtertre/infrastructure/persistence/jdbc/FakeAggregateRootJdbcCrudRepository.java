package vtertre.infrastructure.persistence.jdbc;

import org.springframework.data.repository.CrudRepository;

public interface FakeAggregateRootJdbcCrudRepository extends CrudRepository<FakeAggregateRootDbo, String> {
}
