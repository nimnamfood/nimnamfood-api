package nimnamfood.query.recipe;

import nimnamfood.query.recipe.model.RecipeSummary;
import vtertre.query.Query;

import java.util.UUID;

public class GetRecipe extends Query<RecipeSummary> {
    public final UUID id;

    public GetRecipe(String stringUuid) {
        this.id = UUID.fromString(stringUuid);
    }
}
