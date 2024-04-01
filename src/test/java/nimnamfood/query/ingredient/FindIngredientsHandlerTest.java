package nimnamfood.query.ingredient;

import nimnamfood.infrastructure.repository.jdbc.WithJdbcRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithJdbcRepositories.class})
public class FindIngredientsHandlerTest extends PostgresTestContainerBase {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void returnsAnEmptyListOfIngredients() {
        FindIngredientsHandler handler = new FindIngredientsHandler();

        List<IngredientSummary> result = handler.execute(new FindIngredients(), jdbcTemplate);

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllIngredientsWhenNoQueryIsProvided() {
        FindIngredientsHandler handler = new FindIngredientsHandler();
        Ingredient ingredient1 = new Ingredient("chocolat", IngredientUnit.GRAM);
        Ingredient ingredient2 = new Ingredient("citron", IngredientUnit.PIECE);
        Repositories.ingredients().add(ingredient1);
        Repositories.ingredients().add(ingredient2);

        List<IngredientSummary> result = handler.execute(new FindIngredients(), jdbcTemplate);

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

        List<IngredientSummary> result = handler.execute(new FindIngredients("citron"), jdbcTemplate);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(ingredient3.getId()) &&
                summary.name.equals(ingredient3.getName()) &&
                summary.unit == ingredient3.getUnit());
        assertThat(result).anyMatch(summary -> summary.id.equals(ingredient2.getId()) &&
                summary.name.equals(ingredient2.getName()) &&
                summary.unit == ingredient2.getUnit());
    }

    @Disabled("Désactivé le temps de trouver comment ignorer les caractères spéciaux côté DB ou via les projections")
    @Test
    void ignoresTheQueryCaseAndSpecialCharacters() {
        FindIngredientsHandler handler = new FindIngredientsHandler();
        Repositories.ingredients().add(new Ingredient("Chöcolat", IngredientUnit.GRAM));

        List<IngredientSummary> result = handler.execute(new FindIngredients("choc"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name).isEqualTo("Chöcolat");
    }

    @Test
    void paginatesTheIngredients() {
        FindIngredientsHandler handler = new FindIngredientsHandler();
        Ingredient ingredient1 = new Ingredient("chocolat", IngredientUnit.GRAM);
        Ingredient ingredient2 = new Ingredient("citron", IngredientUnit.PIECE);
        Repositories.ingredients().add(ingredient1);
        Repositories.ingredients().add(ingredient2);

        List<IngredientSummary> result1 = handler.execute((FindIngredients) new FindIngredients().limit(1).skip(0), jdbcTemplate);
        List<IngredientSummary> result2 = handler.execute((FindIngredients) new FindIngredients().limit(1).skip(1), jdbcTemplate);

        assertThat(result1).hasSize(1);
        assertThat(result1.getFirst().id).isEqualTo(ingredient1.getId());
        assertThat(result1.getFirst().name).isEqualTo("chocolat");
        assertThat(result1.getFirst().unit).isEqualTo(IngredientUnit.GRAM);
        assertThat(result2).hasSize(1);
        assertThat(result2.getFirst().id).isEqualTo(ingredient2.getId());
        assertThat(result2.getFirst().name).isEqualTo("citron");
        assertThat(result2.getFirst().unit).isEqualTo(IngredientUnit.PIECE);
    }
}
