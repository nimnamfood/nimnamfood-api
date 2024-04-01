package nimnamfood.query.ingredient.model;

import nimnamfood.model.ingredient.IngredientUnit;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class IngredientSummary {
    public UUID id;
    public String name;
    public IngredientUnit unit;

    public static Mapper mapper() {
        return new Mapper();
    }

    public static class Mapper implements RowMapper<IngredientSummary> {
        private Mapper() {
        }

        @Override
        public IngredientSummary mapRow(ResultSet result, int rowNum) throws SQLException {
            final IngredientSummary summary = new IngredientSummary();
            summary.id = result.getObject("id", UUID.class);
            summary.name = result.getString("name");
            summary.unit = IngredientUnit.valueOf(result.getString("unit"));
            return summary;
        }
    }
}
