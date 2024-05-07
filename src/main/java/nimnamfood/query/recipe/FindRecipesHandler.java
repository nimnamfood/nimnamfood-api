package nimnamfood.query.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.ddd.Tuple;
import vtertre.query.QueryHandlerJdbc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class FindRecipesHandler extends QueryHandlerJdbc<FindRecipes, List<RecipeSearchSummary>> {
    private final ObjectMapper mapper;
    private final TypeReference<Set<TagSummary>> typeReference = new TypeReference<>() {
    };
    private final String tagQueryColumn = "tag_ids";

    @Autowired
    public FindRecipesHandler(@Qualifier("Jsonb") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<RecipeSearchSummary> execute(FindRecipes query, NamedParameterJdbcTemplate template) {
        return this.sqlQuerySpecTuple(query).apply(((sql, paramSource) ->
                template.query(sql, paramSource, resultSet -> {
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
                })));
    }

    private Tuple<String, MapSqlParameterSource> sqlQuerySpecTuple(FindRecipes query) {
        final Map<String, Object> params = this.baseLimitOffsetParams(query);
        params.put("query", "%" + query.query + "%");

        final String sql;
        if (query.tagFilterQuery == null || query.tagFilterQuery.isEmpty()) {
            sql = sqlQuery(query.query);
        } else {
            final Tuple<String, Map<String, Set<String>>> tagQuerySpec =
                    new JdbcTagQueryTranslator(this.tagQueryColumn).toQueryValue(query.tagFilterQuery);

            sql = sqlQuery(query.query, tagQuerySpec._1);
            params.putAll(tagQuerySpec._2);
        }

        return Tuple.of(appendLimitAndOffset(appendOrderBy(sql)), new MapSqlParameterSource(params));
    }

    private String sqlQuery(String searchQuery, String tagsSqlWhereClause) {
        return """
                WITH extended_view AS (
                    SELECT id, name, illustration_url, tags, creation_date_time, jsonb_path_query_array(tags, '$.id') AS\s""" + this.tagQueryColumn + """
                    FROM view_recipe_search
                )
                SELECT id, name, illustration_url, tags, creation_date_time
                FROM extended_view
                WHERE
                """ +
                (Strings.isNullOrEmpty(searchQuery) ? tagsSqlWhereClause :
                        nameFilterSql() + " AND " + tagsSqlWhereClause);
    }

    private static String sqlQuery(String searchQuery) {
        final String selectFrom = "SELECT id, name, illustration_url, tags, creation_date_time FROM view_recipe_search";
        return selectFrom + (Strings.isNullOrEmpty(searchQuery) ? "" : " WHERE " + nameFilterSql());
    }

    private static String nameFilterSql() {
        return "UNACCENT(name) ILIKE UNACCENT(:query)";
    }

    private static String appendOrderBy(String sql) {
        return sql + " ORDER BY creation_date_time DESC";
    }
}
