package nimnamfood.query.plan;

import nimnamfood.query.plan.model.PlanSearchSummary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandlerJdbc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class GetPlansHandler extends QueryHandlerJdbc<GetPlans, List<PlanSearchSummary>> {
    @Override
    protected List<PlanSearchSummary> execute(GetPlans query, NamedParameterJdbcTemplate template) {
        final var sql = "SELECT id, created_at FROM view_plan_search ORDER BY created_at DESC";
        return template.query(appendLimitAndOffset(sql), this.baseLimitOffsetParams(query), resultSet -> {
            final var summaries = new ArrayList<PlanSearchSummary>();

            while (resultSet.next()) {
                final var summary = new PlanSearchSummary(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
                );
                summaries.add(summary);
            }
            return summaries;
        });
    }
}
