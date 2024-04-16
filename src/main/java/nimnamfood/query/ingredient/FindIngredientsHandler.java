package nimnamfood.query.ingredient;

import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandlerJdbc;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
public class FindIngredientsHandler extends QueryHandlerJdbc<FindIngredients, List<IngredientSummary>> {
    @Override
    protected List<IngredientSummary> execute(FindIngredients query, NamedParameterJdbcTemplate template) {
        final String baseSqlQuery = "SELECT * FROM view_ingredients";
        final HashMap<String, Object> params = this.baseLimitOffsetParams(query);

        if (query.query == null || query.query.isEmpty()) {
            final String sqlQuery = appendLimitAndOffset(baseSqlQuery);
            return template.query(sqlQuery, new MapSqlParameterSource(params), ingredientSummaryMapper());
        }

        final String sqlQuery = appendLimitAndOffset(baseSqlQuery + " WHERE UNACCENT(name) ILIKE UNACCENT(:query)");
        params.put("query", "%" + query.query + "%");
        return template.query(sqlQuery, new MapSqlParameterSource(params), ingredientSummaryMapper());
    }

    private static RowMapper<IngredientSummary> ingredientSummaryMapper() {
        return (resultSet, rowNum) -> new IngredientSummary(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                IngredientUnit.valueOf(resultSet.getString("unit"))
        );
    }
}
