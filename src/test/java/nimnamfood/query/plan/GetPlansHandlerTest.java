package nimnamfood.query.plan;

import nimnamfood.model.plan.Plan;
import nimnamfood.query.plan.model.PlanSearchSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Import(PlanSearchViewTestHelper.class)
public class GetPlansHandlerTest extends PostgresTestContainerBase {
    @Autowired
    PlanSearchViewTestHelper view;
    @Autowired
    NamedParameterJdbcTemplate template;

    @Test
    void returnsAnEmptyListOfPlans() {
        var handler = new GetPlansHandler();

        var result = handler.execute(new GetPlans(), template);

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllPlans() {
        var handler = new GetPlansHandler();
        var plan1 = Plan.factory().create(Set.of())._1;
        var plan2 = Plan.factory().create(Set.of())._1;
        view.insert(plan1, plan2);

        var result = handler.execute(new GetPlans(), template);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(summary -> {
            assertThat(summary.id()).isEqualTo(plan1.getId());
            assertThat(summary.createdAt()).isCloseTo(plan1.createdAt().toInstant(ZoneOffset.UTC), within(1, ChronoUnit.MICROS));
        });
        assertThat(result).anySatisfy(summary -> {
            assertThat(summary.id()).isEqualTo(plan2.getId());
            assertThat(summary.createdAt()).isCloseTo(plan2.createdAt().toInstant(ZoneOffset.UTC), within(1, ChronoUnit.MICROS));
        });
    }

    @Test
    void paginatesThePlansInReversedCreationOrder() {
        var handler = new GetPlansHandler();
        var plan1 = Plan.factory().create(Set.of())._1;
        var plan2 = Plan.factory().create(Set.of())._1;
        var plan3 = Plan.factory().create(Set.of())._1;
        view.insert(plan3, plan1, plan2);

        var result = handler.execute((GetPlans) new GetPlans().limit(2).skip(0), template);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).extracting(PlanSearchSummary::id).isEqualTo(plan3.getId());
        assertThat(result.get(1)).extracting(PlanSearchSummary::id).isEqualTo(plan2.getId());
    }
}
