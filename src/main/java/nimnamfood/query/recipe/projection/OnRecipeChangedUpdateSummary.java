package nimnamfood.query.recipe.projection;

import nimnamfood.model.recipe.RecipeChanged;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class OnRecipeChangedUpdateSummary extends RecipeSummaryEventCaptor<RecipeChanged> {
    @Override
    public void execute(RecipeChanged event, JdbcClient client) {
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
                    UPDATE view_recipes
                    SET name = :name, illustration = :illustration::json, portions_count = :portionsCount, instructions = :instructions, ingredients = :ingredients::jsonb, tags = '[]'::jsonb
                    WHERE id = :id
                    """;
        } else {
            return """
                    WITH selected_tags AS (
                        SELECT jsonb_agg(view_part_recipe_tags.*) as tags_jsonb
                        FROM view_part_recipe_tags
                        WHERE id IN (:tagIds)
                    )
                    UPDATE view_recipes
                    SET name = :name, illustration = :illustration::json, portions_count = :portionsCount, instructions = :instructions, ingredients = :ingredients::jsonb, tags = selected_tags.tags_jsonb
                    FROM selected_tags
                    WHERE view_recipes.id = :id
                    """;
        }
    }
}
