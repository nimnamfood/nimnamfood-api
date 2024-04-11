package nimnamfood.query.recipe;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.recipe.model.RecipeIngredientSummary;
import nimnamfood.query.recipe.model.RecipeSummary;
import nimnamfood.query.tag.model.TagSummary;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.query.QueryHandlerJdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@Component
public class GetRecipeHandler extends QueryHandlerJdbc<GetRecipe, RecipeSummary> {
    private final RecipeService recipeService;

    @Autowired
    public GetRecipeHandler(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Override
    public RecipeSummary execute(GetRecipe query, NamedParameterJdbcTemplate template) {
        final String sqlQuery = """
                SELECT r.id, r.name, r.illustration_id, r.portions_count, r.instructions, t.id as "tag_id", t.name as "tag_name", i.id as "ingredient_id", i.name as "ingredient_name", ri.id as "recipe_ingredient_id", ri.quantity as "ingredient_quantity", ri.unit as "ingredient_unit", ri.quantity_fixed as "ingredient_quantity_fixed"
                FROM recipes r
                    LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_id
                    LEFT JOIN ingredients i ON ri.ingredient_id = i.id
                    LEFT JOIN recipe_tags rt ON r.id = rt.recipe_id
                    LEFT JOIN tags t ON rt.tag_id = t.id
                WHERE r.id = :recipeId
                """;

        return template.query(sqlQuery, new MapSqlParameterSource("recipeId", query.id), resultSet -> {
            if (!resultSet.next()) {
                throw new MissingAggregateRootException(query.id);
            }

            final RecipeSummary summary = extractRecipeSummary(resultSet);

            final Map<UUID, RecipeIngredientSummary> ingredientSummariesById = Maps.newHashMap();
            final Map<UUID, TagSummary> tagSummariesById = Maps.newHashMap();

            do {
                final UUID recipeIngredientId = resultSet.getObject("recipe_ingredient_id", UUID.class);
                if (recipeIngredientId != null && !ingredientSummariesById.containsKey(recipeIngredientId)) {
                    final RecipeIngredientSummary ingredientSummary = extractRecipeIngredientSummary(resultSet);
                    ingredientSummariesById.put(recipeIngredientId, ingredientSummary);
                }

                final UUID tagId = resultSet.getObject("tag_id", UUID.class);
                if (tagId != null && !tagSummariesById.containsKey(tagId)) {
                    final TagSummary tagSummary = new TagSummary(tagId, resultSet.getString("tag_name"));
                    tagSummariesById.put(tagId, tagSummary);
                }
            } while (resultSet.next());

            summary.ingredients().addAll(ingredientSummariesById.values());
            summary.tags().addAll(tagSummariesById.values());

            return summary;
        });
    }

    private RecipeSummary extractRecipeSummary(ResultSet resultSet) throws SQLException {
        final UUID illustrationId = resultSet.getObject("illustration_id", UUID.class);
        return new RecipeSummary(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                illustrationId != null ? this.recipeService.illustrationUrl(illustrationId) : null,
                resultSet.getInt("portions_count"),
                resultSet.getString("instructions"),
                Sets.newHashSet(),
                Sets.newHashSet()
        );
    }

    private static RecipeIngredientSummary extractRecipeIngredientSummary(ResultSet resultSet) throws SQLException {
        return new RecipeIngredientSummary(
                resultSet.getObject("ingredient_id", UUID.class),
                resultSet.getString("ingredient_name"),
                resultSet.getFloat("ingredient_quantity"),
                IngredientUnit.valueOf(resultSet.getString("ingredient_unit")),
                resultSet.getBoolean("ingredient_quantity_fixed")
        );
    }
}
