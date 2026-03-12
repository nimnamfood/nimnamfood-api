package nimnamfood.query.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import nimnamfood.model.plan.Plan;
import nimnamfood.query.ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;

@Service
public class PlanSearchViewTestHelper {
    @Autowired
    NamedParameterJdbcTemplate template;

    private final ObjectMapper mapper = ObjectMapperFactory.withSnakeCasePropertyNamingStrategy();

    public void insert(Plan... plans) {
        this.template.batchUpdate(
                "insert into view_plan_search values (:id, :createdAt)",
                Arrays.stream(plans).map(p -> Map.of("id", p.getId(), "createdAt", p.createdAt().atOffset(ZoneOffset.UTC))).toArray(Map[]::new)
        );
    }
}
