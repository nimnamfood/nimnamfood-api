package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientDbo;
import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcRepository;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class IngredientJdbcRepositoryTest extends PostgresTestContainerBase {
    @Autowired
    IngredientJdbcCrudRepository crudRepository;
    @Autowired
    JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void retrievesAnIngredient() {
        IngredientJdbcRepository repository = new IngredientJdbcRepository(crudRepository, jdbcAggregateTemplate);
        IngredientDbo dbo = new IngredientDbo();
        dbo.setId(UUID.randomUUID());
        dbo.setName("rapide");
        dbo.setUnit(IngredientUnit.GRAM);
        this.jdbcAggregateTemplate.insert(dbo);

        Optional<Ingredient> foundIngredient = repository.get(dbo.getId());

        assertThat(foundIngredient).isPresent();
        assertThat(foundIngredient.get().getId()).isEqualTo(dbo.getId());
        assertThat(foundIngredient.get().getName()).isEqualTo("rapide");
        assertThat(foundIngredient.get().getUnit()).isEqualTo(IngredientUnit.GRAM);
    }

    @Test
    void addsAnIngredient() {
        IngredientJdbcRepository repository = new IngredientJdbcRepository(crudRepository, jdbcAggregateTemplate);
        Ingredient ingredient = new Ingredient("citron", IngredientUnit.PIECE);

        repository.add(ingredient);
        IngredientDbo dbo = this.jdbcAggregateTemplate.findById(ingredient.getId(), IngredientDbo.class);

        assertThat(dbo).isNotNull();
        assertThat(dbo.getName()).isEqualTo("citron");
        assertThat(dbo.getUnit()).isEqualTo(IngredientUnit.PIECE);
    }
}
