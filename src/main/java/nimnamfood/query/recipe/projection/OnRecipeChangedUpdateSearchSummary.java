package nimnamfood.query.recipe.projection;

import nimnamfood.model.recipe.RecipeChanged;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnRecipeChangedUpdateSearchSummary implements EventCaptor<RecipeChanged> {
    private final JdbcClient client;
    private final RecipeService recipeService;

    @Autowired
    public OnRecipeChangedUpdateSearchSummary(JdbcClient client, RecipeService recipeService) {
        this.client = client;
        this.recipeService = recipeService;
    }

    @Override
    public void execute(RecipeChanged event) {
        final String sqlQuery = sqlQuery(event.tagIds().isEmpty());

        final String illustrationUrl = event.illustrationId() != null ?
                this.recipeService.illustrationUrl(event.illustrationId()) : null;

        this.client.sql(sqlQuery)
                .param("id", event.id())
                .param("name", event.name())
                .param("illustrationUrl", illustrationUrl)
                .param("tagIds", event.tagIds())
                .update();
    }

    private static String sqlQuery(boolean hasNoTags) {
        if (hasNoTags) {
            return """
                    UPDATE view_recipe_search
                    SET name = :name, illustration_url = :illustrationUrl, tags = '[]'::jsonb
                    WHERE id = :id
                    """;
        } else {
            return """
                    WITH selected_tags AS (
                        SELECT jsonb_agg(view_part_recipe_tags.*) as tags_jsonb
                        FROM view_part_recipe_tags
                        WHERE id IN (:tagIds)
                    )
                    UPDATE view_recipe_search
                    SET name = :name, illustration_url = :illustrationUrl, tags = selected_tags.tags_jsonb
                    FROM selected_tags
                    WHERE view_recipe_search.id = :id
                    """;
        }
    }
}
