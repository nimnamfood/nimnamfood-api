package nimnamfood.query.recipe.projection;

import nimnamfood.model.recipe.RecipeCreated;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class OnRecipeCreatedFillSummary extends RecipeSummaryEventCaptor<RecipeCreated> {
    @Override
    public void execute(RecipeCreated event, JdbcClient client) {
        final String sqlQuery = sqlQuery(event.tagIds().isEmpty());

        final String illustrationJson = event.illustrationId() != null ?
                this.illustrationJsonString(event.illustrationId()) : null;
        final String ingredientsJson = ingredientsJsonString(event.ingredients());

        client.sql(sqlQuery)
                .param("id", event.id())
                .param("name", event.name())
                .param("illustration", illustrationJson)
                .param("portionsCount", event.portionsCount())
                .param("instructions", event.instructions())
                .param("ingredients", ingredientsJson)
                .param("tagIds", event.tagIds())
                .update();
    }

    private static String sqlQuery(boolean hasNoTags) {
        if (hasNoTags) {
            return """
                    INSERT INTO view_recipes (id, name, illustration, portions_count, instructions, ingredients, tags)
                    VALUES(:id, :name, :illustration::json, :portionsCount, :instructions, :ingredients::jsonb, '[]'::jsonb)
                    """;
        } else {
            return """
                    WITH selected_tags AS (
                        SELECT *
                        FROM view_part_recipe_tags
                        WHERE id IN (:tagIds)
                    )
                    INSERT INTO view_recipes (id, name, illustration, portions_count, instructions, ingredients, tags)
                    SELECT :id, :name, :illustration::json, :portionsCount, :instructions, :ingredients::jsonb, jsonb_agg(selected_tags.*)
                    FROM selected_tags
                    """;
        }
    }
}
