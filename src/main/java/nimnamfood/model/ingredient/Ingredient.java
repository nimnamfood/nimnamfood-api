package nimnamfood.model.ingredient;

import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientDbo;
import vtertre.ddd.BaseAggregateRootWithUuid;
import vtertre.ddd.Tuple;

import java.util.UUID;

public class Ingredient extends BaseAggregateRootWithUuid {
    private final String name;
    private final IngredientUnit unit;

    public static Factory factory() {
        return new Factory();
    }

    private Ingredient(String name, IngredientUnit unit) {
        this.name = name;
        this.unit = unit;
    }

    private Ingredient(UUID id, String name, IngredientUnit unit) {
        super(id);
        this.name = name;
        this.unit = unit;
    }

    public static class Factory {
        public Tuple<Ingredient, IngredientCreated> create(String name, IngredientUnit unit) {
            final Ingredient ingredient = new Ingredient(name, unit);
            return Tuple.of(ingredient, new IngredientCreated(ingredient.getId(), name, unit));
        }

        public Ingredient recreateFromDbo(IngredientDbo dbo) {
            return new Ingredient(dbo.getId(), dbo.getName(), dbo.getUnit());
        }
    }

    public Tuple<Ingredient, IngredientChanged> updated(String name, IngredientUnit unit) {
        return Tuple.of(new Ingredient(this.getId(), name, unit), new IngredientChanged(this.getId(), name, unit));
    }

    public String getName() {
        return this.name;
    }

    public IngredientUnit getUnit() {
        return this.unit;
    }
}
