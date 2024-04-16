package nimnamfood.query.ingredient.projection;

import nimnamfood.model.ingredient.IngredientCreated;
import nimnamfood.model.ingredient.IngredientUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OnIngredientCreatedFillSummaryTest extends PostgresTestContainerBase {
    @Autowired
    JdbcClient client;

    @Test
    void insertsTheIngredientIntoTheView() {
        IngredientCreated event = new IngredientCreated(UUID.randomUUID(), "1", IngredientUnit.GRAM);

        new OnIngredientCreatedFillSummary(client).execute(event);
        int result = JdbcTestUtils.countRowsInTableWhere(client, "view_ingredients", "id = '" + event.id() + "' and name = '1' and unit = 'GRAM'");

        assertThat(result).isEqualTo(1);
    }
}