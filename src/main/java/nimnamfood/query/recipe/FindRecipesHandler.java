package nimnamfood.query.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandlerJdbc;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class FindRecipesHandler extends QueryHandlerJdbc<FindRecipes, List<RecipeSearchSummary>> {
    private final ObjectMapper mapper;
    private final TypeReference<Set<TagSummary>> typeReference = new TypeReference<>() {
    };

    @Autowired
    public FindRecipesHandler(@Qualifier("Jsonb") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<RecipeSearchSummary> execute(FindRecipes query, NamedParameterJdbcTemplate template) {
        final String sqlQuery = appendLimitAndOffset(sqlQuery(query));
        final HashMap<String, Object> params = this.baseLimitOffsetParams(query);
        params.put("query", "%" + query.query + "%");
        params.put("tagIds", query.tags);

        return template.query(sqlQuery, new MapSqlParameterSource(params), resultSet -> {
            final List<RecipeSearchSummary> summaries = Lists.newArrayList();
            try {
                while (resultSet.next()) {
                    final RecipeSearchSummary summary = new RecipeSearchSummary(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("name"),
                            resultSet.getString("illustration_url"),
                            mapper.readValue(resultSet.getString("tags"), typeReference)
                    );
                    summaries.add(summary);
                }
                return summaries;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String sqlQuery(FindRecipes query) {
        final String selectFrom = "SELECT id, name, illustration_url, tags, creation_date_time FROM view_recipe_search";
        final String where = whereClause(query);
        final String orderBy = "ORDER BY creation_date_time DESC";
        return selectFrom + " " + where + " " + orderBy;
    }

    private static String whereClause(FindRecipes query) {
        if ((query.query == null || query.query.isEmpty()) && (query.tags == null || query.tags.isEmpty())) {
            return "";
        }
        if (query.tags == null || query.tags.isEmpty()) {
            return "WHERE UNACCENT(name) ILIKE UNACCENT(:query)";
        }
        if (query.query == null || query.query.isEmpty()) {
            return "WHERE jsonb_exists_all(jsonb_path_query_array(tags, '$.id'), array[:tagIds])";
        }
        return "WHERE UNACCENT(name) ILIKE UNACCENT(:query) AND jsonb_exists_all(jsonb_path_query_array(tags, '$.id'), array[:tagIds])";
    }
}
