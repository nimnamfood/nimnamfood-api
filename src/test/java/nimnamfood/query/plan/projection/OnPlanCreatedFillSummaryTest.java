package nimnamfood.query.plan.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import nimnamfood.infrastructure.repository.jdbc.WithJdbcRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.PlanCreated;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.query.ObjectMapperFactory;
import nimnamfood.query.plan.PlanViewTestHelper;
import nimnamfood.query.plan.model.PlanSummary;
import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithJdbcRepositories.class})
@Import(PlanViewTestHelper.class)
class OnPlanCreatedFillSummaryTest extends PostgresTestContainerBase {
    @Autowired
    PlanViewTestHelper view;
    @Autowired
    JdbcClient client;

    ObjectMapper mapper = ObjectMapperFactory.withSnakeCasePropertyNamingStrategy();
    RecipeService recipeService = Mockito.mock();

    @Test
    void insertsThePlanSummary() {
        OnPlanCreatedFillSummary captor = new OnPlanCreatedFillSummary(client, recipeService, mapper);

        Recipe recipe = Recipe.factory().create("omelette", UUID.randomUUID(), 1, ImmutableSet.of(), "", ImmutableSet.of())._1;
        Repositories.recipes().add(recipe);
        Mockito.when(recipeService.illustrationUrl(recipe.getIllustrationId())).thenReturn("url");

        Meal meal = new Meal(0, recipe.getId());
        PlanCreated event = new PlanCreated(UUID.randomUUID(), LocalDateTime.now(), ImmutableSet.of(meal));

        captor.execute(event);

        PlanSummary summary = view.find(event.id());
        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(event.id());
        assertThat(summary.meals()).hasSize(1);
        assertThat(summary.meals()).first().satisfies(ms -> {
            assertThat(ms.mealIndex()).isEqualTo(0);
            assertThat(ms.recipe().id()).isEqualTo(recipe.getId());
            assertThat(ms.recipe().name()).isEqualTo("omelette");
            assertThat(ms.recipe().illustrationUrl()).isEqualTo("url");
        });
    }

    @Test
    void mealsCanBeEmpty() {
        OnPlanCreatedFillSummary captor = new OnPlanCreatedFillSummary(client, recipeService, mapper);

        PlanCreated event = new PlanCreated(UUID.randomUUID(), LocalDateTime.now(), ImmutableSet.of());

        captor.execute(event);

        PlanSummary summary = view.find(event.id());
        assertThat(summary).isNotNull();
        assertThat(summary.meals()).isEmpty();
    }
}
