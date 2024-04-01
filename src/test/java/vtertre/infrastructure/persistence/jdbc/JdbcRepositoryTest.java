package vtertre.infrastructure.persistence.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(FakeAggregateRootJdbcRepository.class)
public class JdbcRepositoryTest extends PostgresTestContainerBase {
    @Autowired
    JdbcAggregateTemplate jdbcAggregateTemplate;
    @Autowired
    FakeAggregateRootJdbcRepository repository;

    @Test
    void canRetrieveAnAggregate() {
        FakeAggregateRoot aggregateRoot = new FakeAggregateRoot("1", "aggregate");
        FakeAggregateRootDbo dbo = new FakeAggregateRootDbo();
        dbo.id = aggregateRoot.id;
        dbo.name = aggregateRoot.name;
        this.jdbcAggregateTemplate.insert(dbo);

        Optional<FakeAggregateRoot> foundAggregateRoot = this.repository.get("1");

        assertThat(foundAggregateRoot).isPresent();
        assertThat(foundAggregateRoot.get().name).isEqualTo("aggregate");
    }

    @Test
    void canAddAnAggregate() {
        FakeAggregateRoot aggregateRoot = new FakeAggregateRoot("1", "aggregate");

        this.repository.add(aggregateRoot);
        FakeAggregateRootDbo dbo = this.jdbcAggregateTemplate.findById("1", FakeAggregateRootDbo.class);

        assertThat(dbo).isNotNull();
    }

    @Test
    void canCheckIfAnAggregateRootExistsByItsId() {
        FakeAggregateRoot aggregateRoot = new FakeAggregateRoot("1", "aggregate");
        FakeAggregateRootDbo dbo = new FakeAggregateRootDbo();
        dbo.id = aggregateRoot.id;
        dbo.name = aggregateRoot.name;
        this.jdbcAggregateTemplate.insert(dbo);

        boolean addedAggregateExists = this.repository.exists("1");
        boolean randomAggregateDoesNotExist = this.repository.exists("2");

        assertThat(addedAggregateExists).isTrue();
        assertThat(randomAggregateDoesNotExist).isFalse();
    }

    private static class Main {
        public static void main(String[] args) {
            SpringApplication.run(FakeApp.class, args);
        }
    }

    @SpringBootApplication
    public static class FakeApp {
    }
}
