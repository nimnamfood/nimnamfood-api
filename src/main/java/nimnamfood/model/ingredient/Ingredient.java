package nimnamfood.model.ingredient;

import vtertre.ddd.BaseAggregateRootWithUuid;

import java.util.UUID;

public class Ingredient extends BaseAggregateRootWithUuid {
    private final String name;
    private final IngredientUnit unit;

    public Ingredient(String name, IngredientUnit unit) {
        this.name = name;
        this.unit = unit;
    }

    public Ingredient(UUID id, String name, IngredientUnit unit) {
        super(id);
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
