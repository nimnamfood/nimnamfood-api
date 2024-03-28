package nimnamfood.infrastructure.repository.jdbc.recipe;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface RecipeJdbcCrudRepository extends CrudRepository<RecipeDbo, UUID> {
}
