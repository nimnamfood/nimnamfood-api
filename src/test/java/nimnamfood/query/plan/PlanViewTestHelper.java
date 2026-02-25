package nimnamfood.query.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nimnamfood.query.ObjectMapperFactory;
import nimnamfood.query.plan.model.PlanSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlanViewTestHelper {
    @Autowired
    JdbcClient client;

    private final ObjectMapper mapper = ObjectMapperFactory.withSnakeCasePropertyNamingStrategy();

    public void insert(PlanSummary summary) {
        final String mealsJson;
        try {
            mealsJson = mapper.writeValueAsString(summary.meals());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        client.sql("INSERT INTO view_plans (id, meals) VALUES (:id, :meals::jsonb)")
                .param("id", summary.id())
                .param("meals", mealsJson)
                .update();
    }

    public PlanSummary find(UUID id) {
        return client.sql("SELECT id, meals FROM view_plans WHERE id = :id")
                .param("id", id)
                .query(resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }
                    try {
                        return new PlanSummary(
                                resultSet.getObject("id", UUID.class),
                                mapper.readValue(resultSet.getString("meals"), new TypeReference<>() {
                                })
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
