package nimnamfood.query.recipe.projection;

import nimnamfood.model.recipe.RecipeCreated;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

import java.time.ZoneOffset;

@Component
public class OnRecipeCreatedFillSearchSummary implements EventCaptor<RecipeCreated> {
    private final JdbcClient client;
    private final RecipeService recipeService;

    @Autowired
    public OnRecipeCreatedFillSearchSummary(JdbcClient client, RecipeService recipeService) {
        this.client = client;
        this.recipeService = recipeService;
    }

    @Override
    public void execute(RecipeCreated event) {
        final String sqlQuery = sqlQuery(event.tagIds().isEmpty());

        final String illustrationUrl = event.illustrationId() != null ?
                this.recipeService.illustrationUrl(event.illustrationId()) : null;

        this.client.sql(sqlQuery)
                .param("id", event.id())
                .param("name", event.name())
                .param("illustrationUrl", illustrationUrl)
                .param("creationDateTime", event.creationDateTime().atOffset(ZoneOffset.UTC))
                .param("tagIds", event.tagIds())
                .update();
    }

    private static String sqlQuery(boolean hasNoTags) {
        if (hasNoTags) {
            return """
                    INSERT INTO view_recipe_search (id, name, illustration_url, creation_date_time, tags)
                    VALUES(:id, :name, :illustrationUrl, :creationDateTime, '[]'::jsonb)
                    """;
        } else {
            return """
                    WITH selected_tags AS (
                        SELECT *
                        FROM view_part_recipe_tags
                        WHERE id IN (:tagIds)
                    )
                    INSERT INTO view_recipe_search (id, name, illustration_url, creation_date_time, tags)
                    SELECT :id, :name, :illustrationUrl, :creationDateTime, jsonb_agg(selected_tags.*)
                    FROM selected_tags
                    """;
        }
    }
}
