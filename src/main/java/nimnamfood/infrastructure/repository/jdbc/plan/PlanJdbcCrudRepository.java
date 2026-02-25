package nimnamfood.infrastructure.repository.jdbc.plan;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface PlanJdbcCrudRepository extends CrudRepository<PlanDbo, UUID> {
}
