package nimnamfood.query.ingredient;

import nimnamfood.model.ingredient.IngredientUnit;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.Set;

@Component
public class GetAllIngredientUnitsHandler implements QueryHandler<GetAllIngredientUnits, Set<IngredientUnit>> {
    @Override
    public Set<IngredientUnit> execute(GetAllIngredientUnits query) {
        return Set.of(IngredientUnit.values());
    }
}
