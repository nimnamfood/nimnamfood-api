package nimnamfood.query.plan.projection;

import nimnamfood.model.recipe.RecipeChanged;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

@Component
public class OnRecipeChangedUpdatePlanSummary implements EventCaptor<RecipeChanged> {
    private final JdbcClient client;
    private final RecipeService recipeService;

    @Autowired
    public OnRecipeChangedUpdatePlanSummary(JdbcClient client, RecipeService recipeService) {
        this.client = client;
        this.recipeService = recipeService;
    }

    @Override
    public void execute(RecipeChanged event) {
        final String illustrationUrl = event.illustrationId() != null
                ? recipeService.illustrationUrl(event.illustrationId())
                : null;

        client.sql("""
                        UPDATE view_plans
                        SET meals = (
                            SELECT jsonb_agg(
                                CASE
                                    WHEN (meal -> 'recipe' ->> 'id') = :recipeId::text
                                    THEN jsonb_set(meal, '{recipe}', jsonb_build_object(
                                        'id', (meal -> 'recipe' ->> 'id'),
                                        'name', :name::text,
                                        'illustration_url', :illustrationUrl::text
                                    ))
                                    ELSE meal
                                END
                            )
                            FROM jsonb_array_elements(meals) AS meal
                        )
                        WHERE meals @> jsonb_build_array(jsonb_build_object('recipe', jsonb_build_object('id', :recipeId::text)))
                        """)
                .param("recipeId", event.id())
                .param("name", event.name())
                .param("illustrationUrl", illustrationUrl)
                .update();
    }

}
