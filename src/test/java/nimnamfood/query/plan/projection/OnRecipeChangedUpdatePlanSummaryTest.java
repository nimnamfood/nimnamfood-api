package nimnamfood.query.plan.projection;

import com.google.common.collect.ImmutableSet;
import nimnamfood.model.recipe.RecipeChanged;
import nimnamfood.query.plan.PlanViewTestHelper;
import nimnamfood.query.plan.model.MealRecipeSummary;
import nimnamfood.query.plan.model.MealSummary;
import nimnamfood.query.plan.model.PlanSummary;
import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PlanViewTestHelper.class)
class OnRecipeChangedUpdatePlanSummaryTest extends PostgresTestContainerBase {
    @Autowired
    PlanViewTestHelper view;
    @Autowired
    JdbcClient client;

    RecipeService recipeService = Mockito.mock();

    @Test
    void updatesTheRecipeDataInAllMatchingMeals() {
        UUID planId = UUID.randomUUID();

        MealSummary meal1 = new MealSummary(0, new MealRecipeSummary(UUID.randomUUID(), "Old name", "old-url"));
        MealSummary meal2 = new MealSummary(1, new MealRecipeSummary(UUID.randomUUID(), "Other recipe", null));
        view.insert(new PlanSummary(planId, Set.of(meal2, meal1)));

        RecipeChanged event = new RecipeChanged(meal1.recipe().id(), "New name", UUID.randomUUID(), 1, "", ImmutableSet.of(), ImmutableSet.of());
        Mockito.when(recipeService.illustrationUrl(event.illustrationId())).thenReturn("new-url");

        new OnRecipeChangedUpdatePlanSummary(client, recipeService).execute(event);

        PlanSummary updated = view.find(planId);
        assertThat(updated.meals()).anyMatch(ms -> ms.recipe().name().equals("New name") && ms.recipe().illustrationUrl().equals("new-url"));
        assertThat(updated.meals()).anyMatch(ms -> ms.recipe().name().equals("Other recipe") && ms.recipe().illustrationUrl() == null);
    }

    @Test
    void updatesAllPlansThatReferenceTheRecipe() {
        UUID recipeId = UUID.randomUUID();
        UUID plan1Id = UUID.randomUUID();
        UUID plan2Id = UUID.randomUUID();

        view.insert(new PlanSummary(plan1Id, Set.of(new MealSummary(0, new MealRecipeSummary(recipeId, "Old", null)))));
        view.insert(new PlanSummary(plan2Id, Set.of(new MealSummary(1, new MealRecipeSummary(recipeId, "Old", null)))));

        RecipeChanged event = new RecipeChanged(recipeId, "Updated", null, 1, "", ImmutableSet.of(), ImmutableSet.of());

        new OnRecipeChangedUpdatePlanSummary(client, recipeService).execute(event);

        assertThat(view.find(plan1Id).meals()).first()
                .satisfies(ms -> assertThat(ms.recipe().name()).isEqualTo("Updated"));
        assertThat(view.find(plan2Id).meals()).first()
                .satisfies(ms -> assertThat(ms.recipe().name()).isEqualTo("Updated"));
    }

    @Test
    void doesNotUpdatePlansThatDoNotReferenceTheRecipe() {
        UUID recipeId = UUID.randomUUID();
        UUID unrelatedRecipeId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        view.insert(new PlanSummary(planId, Set.of(new MealSummary(0, new MealRecipeSummary(unrelatedRecipeId, "Untouched", null)))));

        RecipeChanged event = new RecipeChanged(recipeId, "Changed", null, 1, "", ImmutableSet.of(), ImmutableSet.of());

        new OnRecipeChangedUpdatePlanSummary(client, recipeService).execute(event);

        PlanSummary unchanged = view.find(planId);
        assertThat(unchanged.meals()).first()
                .satisfies(ms -> assertThat(ms.recipe().name()).isEqualTo("Untouched"));
    }
}
