package nimnamfood.infrastructure.repository.jdbc.ingredient;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface IngredientJdbcCrudRepository extends CrudRepository<IngredientDbo, UUID> {
}
