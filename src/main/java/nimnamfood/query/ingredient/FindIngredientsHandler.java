package nimnamfood.query.ingredient;

import nimnamfood.query.ingredient.model.IngredientSummary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandlerJdbc;

import java.util.HashMap;
import java.util.List;

@Component
public class FindIngredientsHandler extends QueryHandlerJdbc<FindIngredients, List<IngredientSummary>> {
    @Override
    protected List<IngredientSummary> execute(FindIngredients query, NamedParameterJdbcTemplate template) {
        final String baseSqlQuery = "SELECT * FROM ingredients";
        final HashMap<String, Object> params = this.baseLimitOffsetParams(query);

        if (query.query == null || query.query.isEmpty()) {
            final String sqlQuery = appendLimitAndOffset(baseSqlQuery);
            return template.query(sqlQuery, new MapSqlParameterSource(params), IngredientSummary.mapper());
        }

        final String sqlQuery = appendLimitAndOffset(baseSqlQuery + " WHERE name ILIKE :query");
        params.put("query", "%" + query.query + "%");
        return template.query(sqlQuery, new MapSqlParameterSource(params), IngredientSummary.mapper());
    }
}
