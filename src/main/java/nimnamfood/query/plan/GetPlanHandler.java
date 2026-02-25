package nimnamfood.query.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nimnamfood.query.plan.model.MealSummary;
import nimnamfood.query.plan.model.PlanSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.query.QueryHandlerJdbc;

import java.util.Set;
import java.util.UUID;

@Component
public class GetPlanHandler extends QueryHandlerJdbc<GetPlan, PlanSummary> {
    private final ObjectMapper mapper;
    private final TypeReference<Set<MealSummary>> mealsTypeReference = new TypeReference<>() {};

    @Autowired
    public GetPlanHandler(@Qualifier("Jsonb") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected PlanSummary execute(GetPlan query, NamedParameterJdbcTemplate template) {
        final String sql = "SELECT id, meals FROM view_plans WHERE id = :planId";

        return template.query(sql, new MapSqlParameterSource("planId", query.id), resultSet -> {
            if (!resultSet.next()) {
                throw new MissingAggregateRootException(query.id);
            }

            try {
                return new PlanSummary(
                        resultSet.getObject("id", UUID.class),
                        mapper.readValue(resultSet.getString("meals"), mealsTypeReference)
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
