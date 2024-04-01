package nimnamfood.query.ingredient;

import nimnamfood.model.ingredient.IngredientUnit;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GetAllIngredientUnitsHandlerTest {

    @Test
    void returnsASetOfIngredientUnits() {
        GetAllIngredientUnitsHandler handler = new GetAllIngredientUnitsHandler();

        Set<IngredientUnit> result = handler.execute(new GetAllIngredientUnits());

        assertThat(result).containsExactlyInAnyOrder(
                IngredientUnit.GRAM,
                IngredientUnit.MILLILITER,
                IngredientUnit.PIECE,
                IngredientUnit.TABLESPOON,
                IngredientUnit.TEASPOON,
                IngredientUnit.PINCH
        );
    }
}
