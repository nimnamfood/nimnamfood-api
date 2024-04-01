package nimnamfood.query.recipe;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.recipe.model.RecipeIngredientSummary;
import nimnamfood.query.recipe.model.RecipeSummary;
import nimnamfood.query.tag.model.TagSummary;
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
    @Override
    public RecipeSummary execute(GetRecipe query, NamedParameterJdbcTemplate template) {
        final String sqlQuery = """
                SELECT r.id, r.name, r.portions_count, r.instructions, t.id as "tag_id", t.name as "tag_name", i.id as "ingredient_id", i.name as "ingredient_name", ri.id as "recipe_ingredient_id", ri.quantity as "ingredient_quantity", ri.unit as "ingredient_unit", ri.quantity_fixed as "ingredient_quantity_fixed"
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
                if (!ingredientSummariesById.containsKey(recipeIngredientId)) {
                    final RecipeIngredientSummary ingredientSummary = extractRecipeIngredientSummary(resultSet);
                    ingredientSummariesById.put(recipeIngredientId, ingredientSummary);
                }

                final UUID tagId = resultSet.getObject("tag_id", UUID.class);
                if (!tagSummariesById.containsKey(tagId)) {
                    final TagSummary tagSummary = new TagSummary();
                    tagSummary.id = tagId;
                    tagSummary.name = resultSet.getString("tag_name");
                    tagSummariesById.put(tagId, tagSummary);
                }
            } while (resultSet.next());

            summary.ingredients = Sets.newHashSet(ingredientSummariesById.values());
            summary.tags = Sets.newHashSet(tagSummariesById.values());

            return summary;
        });
    }

    private static RecipeSummary extractRecipeSummary(ResultSet resultSet) throws SQLException {
        final RecipeSummary summary = new RecipeSummary();
        summary.id = resultSet.getObject("id", UUID.class);
        summary.name = resultSet.getString("name");
        summary.portionsCount = resultSet.getInt("portions_count");
        summary.instructions = resultSet.getString("instructions");
        return summary;
    }

    private static RecipeIngredientSummary extractRecipeIngredientSummary(ResultSet resultSet) throws SQLException {
        final RecipeIngredientSummary ingredientSummary = new RecipeIngredientSummary();
        ingredientSummary.id = resultSet.getObject("ingredient_id", UUID.class);
        ingredientSummary.name = resultSet.getString("ingredient_name");
        ingredientSummary.quantity = resultSet.getFloat("ingredient_quantity");
        ingredientSummary.unit = IngredientUnit.valueOf(resultSet.getString("ingredient_unit"));
        ingredientSummary.quantityFixed = resultSet.getBoolean("ingredient_quantity_fixed");
        return ingredientSummary;
    }
}
