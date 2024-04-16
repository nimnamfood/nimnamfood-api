package nimnamfood.query.ingredient;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngredientsViewTestHelper {
    @Autowired
    NamedParameterJdbcTemplate template;
    @Autowired
    JdbcClient client;

    public void insertIngredients(Ingredient... ingredients) {
        this.template.batchUpdate(
                "insert into view_ingredients values (:id, :name, :unit)",
                Arrays.stream(ingredients).map(i -> Map.of(
                        "id", i.getId(),
                        "name", i.getName(),
                        "unit", i.getUnit().toString()
                )).toArray(Map[]::new)
        );
    }

    public Optional<IngredientSummary> findIngredient(UUID id) {
        return this.client
                .sql("select * from view_ingredients where id = :id")
                .param("id", id)
                .query((resultSet, rowNum) -> new IngredientSummary(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name"),
                        IngredientUnit.valueOf(resultSet.getString("unit"))
                )).optional();
    }
}
