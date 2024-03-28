package nimnamfood.infrastructure.repository.jdbc.ingredient;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientRepository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.JdbcRepositoryWithUuid;

public class IngredientJdbcRepository extends JdbcRepositoryWithUuid<Ingredient, IngredientDbo> implements IngredientRepository {

    public IngredientJdbcRepository(IngredientJdbcCrudRepository jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        super(jdbcCrudRepository, jdbcAggregateTemplate);
    }

    @Override
    public IngredientDbo toDbo(Ingredient ingredient) {
        final IngredientDbo dbo = new IngredientDbo();
        dbo.setId(ingredient.getId());
        dbo.name = ingredient.getName();
        dbo.unit = ingredient.getUnit();
        return dbo;
    }
}
