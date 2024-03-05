package nimnamfood.query;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.ingredient.FindIngredients;
import nimnamfood.query.ingredient.FindIngredientsHandler;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class FindIngredientsHandlerTest {

    @Test
    void returnsAnEmptyListOfIngredients() {
        FindIngredientsHandler handler = new FindIngredientsHandler();

        List<IngredientSummary> result = handler.execute(new FindIngredients());

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllIngredientsWhenNoQueryIsProvided() {
        FindIngredientsHandler handler = new FindIngredientsHandler();
        Ingredient ingredient1 = new Ingredient("chocolat", IngredientUnit.GRAM);
        Ingredient ingredient2 = new Ingredient("citron", IngredientUnit.PIECE);
        Repositories.ingredients().add(ingredient1);
        Repositories.ingredients().add(ingredient2);

        List<IngredientSummary> result = handler.execute(new FindIngredients());

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(ingredient1.getId()) &&
                summary.name.equals(ingredient1.getName()) &&
                summary.unit == ingredient1.getUnit());
        assertThat(result).anyMatch(summary -> summary.id.equals(ingredient2.getId()) &&
                summary.name.equals(ingredient2.getName()) &&
                summary.unit == ingredient2.getUnit());
    }

    @Test
    void returnsAllIngredientsContainingTheQuery() {
        FindIngredientsHandler handler = new FindIngredientsHandler();

        Ingredient ingredient1 = new Ingredient("chocolat", IngredientUnit.GRAM);
        Ingredient ingredient2 = new Ingredient("citron", IngredientUnit.PIECE);
        Ingredient ingredient3 = new Ingredient("jus de citron", IngredientUnit.MILLILITER);
        Repositories.ingredients().add(ingredient1);
        Repositories.ingredients().add(ingredient2);
        Repositories.ingredients().add(ingredient3);

        List<IngredientSummary> result = handler.execute(new FindIngredients("citron"));

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(ingredient3.getId()) &&
                summary.name.equals(ingredient3.getName()) &&
                summary.unit == ingredient3.getUnit());
        assertThat(result).anyMatch(summary -> summary.id.equals(ingredient2.getId()) &&
                summary.name.equals(ingredient2.getName()) &&
                summary.unit == ingredient2.getUnit());
    }

    @Test
    void ignoresTheQueryCase() {
        FindIngredientsHandler handler = new FindIngredientsHandler();
        Repositories.ingredients().add(new Ingredient("Chocolat", IngredientUnit.GRAM));

        List<IngredientSummary> result = handler.execute(new FindIngredients("choc"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name).isEqualTo("Chocolat");
    }
}
