package nimnamfood.model.ingredient;

import vtertre.ddd.BaseAggregateRootWithUuid;

public class Ingredient extends BaseAggregateRootWithUuid {
    private final String name;
    private final IngredientUnit unit;

    public Ingredient(String name, IngredientUnit unit) {
        this.name = name;
        this.unit = unit;
    }

    public String getName() {
        return this.name;
    }

    public IngredientUnit getUnit() {
        return this.unit;
    }
}
