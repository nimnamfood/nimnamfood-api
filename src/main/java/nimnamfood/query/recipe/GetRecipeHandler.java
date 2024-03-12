package nimnamfood.query.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.query.recipe.model.RecipeSummary;
import org.springframework.stereotype.Component;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.query.QueryHandler;

@Component
public class GetRecipeHandler implements QueryHandler<GetRecipe, RecipeSummary> {
    @Override
    public RecipeSummary execute(GetRecipe query) {
        return Repositories.recipes()
                .get(query.id)
                .map(RecipeSummary::fromRecipe)
                .orElseThrow(() -> new MissingAggregateRootException(query.id));
    }
}
