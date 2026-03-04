package nimnamfood.query.plan.projection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nimnamfood.model.Repositories;
import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.PlanCreated;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.plan.model.MealRecipeSummary;
import nimnamfood.query.plan.model.MealSummary;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vtertre.ddd.event.EventCaptor;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class OnPlanCreatedFillSummary implements EventCaptor<PlanCreated> {
    private final JdbcClient client;
    private final RecipeService recipeService;
    private final ObjectMapper mapper;

    @Autowired
    public OnPlanCreatedFillSummary(JdbcClient client, RecipeService recipeService, @Qualifier("Jsonb") ObjectMapper mapper) {
        this.client = client;
        this.recipeService = recipeService;
        this.mapper = mapper;
    }

    @Override
    public void execute(PlanCreated event) {
        final Set<UUID> recipeIds = event.meals().stream()
                .map(Meal::recipeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Iterable<Recipe> recipes = recipeIds.isEmpty()
                ? Set.of()
                : Repositories.recipes().getAllById(recipeIds);
        final Map<UUID, Recipe> recipesById = StreamSupport
                .stream(recipes.spliterator(), false)
                .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));

        final Set<MealSummary> mealSummaries = event.meals().stream().map(meal -> {
            if (meal.recipeId() == null) {
                return new MealSummary(meal.mealIndex(), null);
            }

            final Recipe recipe = recipesById.get(meal.recipeId());
            final String illustrationUrl = recipe.getIllustrationId() != null ?
                    this.recipeService.illustrationUrl(recipe.getIllustrationId()) : null;
            return new MealSummary(
                    meal.mealIndex(),
                    new MealRecipeSummary(recipe.getId(), recipe.getName(), illustrationUrl)
            );
        }).collect(Collectors.toSet());

        final String mealsJson;
        try {
            mealsJson = mapper.writeValueAsString(mealSummaries);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        client.sql("INSERT INTO view_plans (id, meals) VALUES (:id, :meals::jsonb)")
                .param("id", event.id())
                .param("meals", mealsJson)
                .update();
    }
}
