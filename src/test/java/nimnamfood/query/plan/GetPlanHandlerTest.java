package nimnamfood.query.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import nimnamfood.query.ObjectMapperFactory;
import nimnamfood.query.plan.model.MealRecipeSummary;
import nimnamfood.query.plan.model.MealSummary;
import nimnamfood.query.plan.model.PlanSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Import(PlanViewTestHelper.class)
public class GetPlanHandlerTest extends PostgresTestContainerBase {
    @Autowired
    PlanViewTestHelper view;
    @Autowired
    NamedParameterJdbcTemplate template;

    ObjectMapper mapper = ObjectMapperFactory.withSnakeCasePropertyNamingStrategy();

    @Test
    void returnsThePlanWithMatchingId() {
        GetPlanHandler handler = new GetPlanHandler(mapper);

        MealSummary mealSummary = new MealSummary(0, new MealRecipeSummary(UUID.randomUUID(), "name", "url"));
        PlanSummary planSummary = new PlanSummary(UUID.randomUUID(), Set.of(mealSummary));
        view.insert(planSummary);

        PlanSummary summary = handler.execute(new GetPlan(planSummary.id().toString()), template);

        assertThat(summary.id()).isEqualTo(planSummary.id());
        assertThat(summary.meals()).hasSize(1).first().satisfies(ms -> {
            assertThat(ms.mealIndex()).isEqualTo(0);
            assertThat(ms.recipe().id()).isEqualTo(mealSummary.recipe().id());
            assertThat(ms.recipe().name()).isEqualTo("name");
            assertThat(ms.recipe().illustrationUrl()).isEqualTo("url");
        });
    }

    @Test
    void throwsAnExceptionWhenTheProvidedIdDoesNotMatchAnyEntity() {
        GetPlanHandler handler = new GetPlanHandler(mapper);
        String stringUuid = UUID.randomUUID().toString();

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(new GetPlan(stringUuid), template))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + stringUuid);
    }
}
