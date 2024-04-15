package nimnamfood.query.recipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import nimnamfood.query.tag.model.TagSummary;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandlerJdbc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FindRecipesHandler extends QueryHandlerJdbc<FindRecipes, List<RecipeSearchSummary>> {
    private final RecipeService recipeService;

    @Autowired
    public FindRecipesHandler(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Override
    public List<RecipeSearchSummary> execute(FindRecipes query, NamedParameterJdbcTemplate template) {
        final String sqlQuery = sqlQuery(query);
        final HashMap<String, Object> params = this.baseLimitOffsetParams(query);
        params.put("query", "%" + query.query + "%");

        if (query.tags != null && !query.tags.isEmpty()) {
            params.put("tagIds", query.tags.stream().map(UUID::fromString).collect(Collectors.toSet()));
            params.put("tagCount", query.tags.size());
        }

        return template.query(sqlQuery, new MapSqlParameterSource(params), resultSet -> {
            final Map<UUID, RecipeSearchSummary> summariesById = Maps.newLinkedHashMap();
            while (resultSet.next()) {
                final UUID id = resultSet.getObject("id", UUID.class);
                final String name = resultSet.getString("name");
                final UUID illustrationId = resultSet.getObject("illustration_id", UUID.class);
                final RecipeSearchSummary summary = summariesById.computeIfAbsent(id, idToAdd ->
                        new RecipeSearchSummary(
                                idToAdd,
                                name,
                                illustrationId != null ? this.recipeService.illustrationUrl(illustrationId) : null,
                                Sets.newHashSet()
                        ));

                final UUID tagId = resultSet.getObject("tag_id", UUID.class);
                if (tagId != null) {
                    final TagSummary tagSummary = new TagSummary(tagId, resultSet.getString("tag_name"));
                    summary.tags().add(tagSummary);
                }
            }
            return Lists.newArrayList(summariesById.values());
        });
    }

    private static String sqlQuery(FindRecipes query) {
        final String innerTableQuery = appendLimitAndOffset(innerTableQuery(query));
        return "WITH matched_recipes AS (" + innerTableQuery + ") SELECT mr.id, mr.name, mr.illustration_id, mr.creation_date_time, t.id as \"tag_id\", t.name as \"tag_name\" FROM matched_recipes mr LEFT JOIN recipe_tags rt ON mr.id = rt.recipe_id LEFT JOIN tags t ON rt.tag_id = t.id ORDER BY mr.creation_date_time DESC";
    }

    private static String innerTableQuery(FindRecipes query) {
        if ((query.query == null || query.query.isEmpty()) && (query.tags == null || query.tags.isEmpty())) {
            return "SELECT DISTINCT(id), name, illustration_id, creation_date_time FROM recipes ORDER BY creation_date_time DESC";
        }
        if (query.tags == null || query.tags.isEmpty()) {
            return "SELECT DISTINCT(id), name, illustration_id, creation_date_time FROM recipes WHERE UNACCENT(name) ILIKE UNACCENT(:query) ORDER BY creation_date_time DESC";
        }
        if (query.query == null || query.query.isEmpty()) {
            return "SELECT DISTINCT(r.id), r.name, r.illustration_id, r.creation_date_time FROM recipes r LEFT JOIN recipe_tags rt ON r.id = rt.recipe_id WHERE rt.tag_id IN (:tagIds) GROUP BY r.id HAVING COUNT(*) = :tagCount ORDER BY r.creation_date_time DESC";
        }
        return "SELECT DISTINCT(r.id), r.name, r.illustration_id, r.creation_date_time FROM recipes r LEFT JOIN recipe_tags rt ON r.id = rt.recipe_id WHERE UNACCENT(r.name) ILIKE UNACCENT(:query) AND rt.tag_id IN (:tagIds) GROUP BY r.id HAVING COUNT(*) = :tagCount ORDER BY r.creation_date_time DESC";
    }
}
