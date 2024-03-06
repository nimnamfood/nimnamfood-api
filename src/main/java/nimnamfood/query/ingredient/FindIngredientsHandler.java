package nimnamfood.query.ingredient;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.query.QueryNormalizer;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.stream.Stream;

@Component
public class FindIngredientsHandler implements QueryHandler<FindIngredients, List<IngredientSummary>> {
    @Override
    public List<IngredientSummary> execute(FindIngredients query) {
        Stream<Ingredient> sourceStream = Repositories.ingredients().getAll().stream();
        Stream<Ingredient> filteredStream = query.query == null ? sourceStream : sourceStream.filter(ingredient ->
                QueryNormalizer.normalize(ingredient.getName()).contains(QueryNormalizer.normalize(query.query)));

        return filteredStream
                .map(IngredientSummary::fromIngredient)
                .toList();
    }
}
