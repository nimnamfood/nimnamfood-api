package nimnamfood.query.ingredient;

import nimnamfood.model.Repositories;
import nimnamfood.query.QueryNormalizer;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.List;

@Component
public class FindIngredientsHandler implements QueryHandler<FindIngredients, List<IngredientSummary>> {
    @Override
    public List<IngredientSummary> execute(FindIngredients query) {
        return Repositories.ingredients()
                .getAll(ingredient -> query.query == null || QueryNormalizer.partialMatch(ingredient.getName(), query.query))
                .stream()
                .map(IngredientSummary::fromIngredient)
                .toList();
    }
}
