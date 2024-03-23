package nimnamfood.query.ingredient;

import nimnamfood.query.ingredient.model.IngredientSummary;
import vtertre.query.Query;

import java.util.List;

public class FindIngredients extends Query<List<IngredientSummary>> {
    public final String query;

    public FindIngredients() {
        this.query = null;
    }

    public FindIngredients(String query) {
        this.query = query;
    }
}
