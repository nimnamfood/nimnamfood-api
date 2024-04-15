package nimnamfood.infrastructure.repository.jdbc.ingredient;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import org.springframework.data.relational.core.mapping.Table;
import vtertre.infrastructure.persistence.jdbc.BaseJdbcDboWithUuid;

@Table("ingredients")
public class IngredientDbo extends BaseJdbcDboWithUuid<Ingredient> {
    String name;
    IngredientUnit unit;

    @Override
    public Ingredient asAggregateRoot() {
        return new Ingredient(this.getId(), this.name, this.unit);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IngredientUnit getUnit() {
        return unit;
    }

    public void setUnit(IngredientUnit unit) {
        this.unit = unit;
    }
}
