package nimnamfood.command.ingredient;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith({WithMemoryRepositories.class})
class UpdateIngredientCommandHandlerTest {
    @Test
    void updatesTheIngredient() {
        UpdateIngredientCommandHandler handler = new UpdateIngredientCommandHandler();
        Ingredient chocolat = new Ingredient("chocolat", IngredientUnit.GRAM);
        Repositories.ingredients().add(chocolat);

        UpdateIngredientCommand command = new UpdateIngredientCommand();
        command.id = chocolat.getId();
        command.name = "chocolat updated";
        command.unit = IngredientUnit.TEASPOON;

        Tuple<Void, List<DomainEvent>> result = handler.execute(command);
        Ingredient ingredient = Repositories.ingredients().get(chocolat.getId()).get();

        assertThat(ingredient.getName()).isEqualTo("chocolat updated");
        assertThat(ingredient.getUnit()).isEqualTo(IngredientUnit.TEASPOON);
        assertThat(result._2).isEmpty();
    }

    @Test
    void throwsAnExceptionIfTheIngredientDoesNotExist() {
        UpdateIngredientCommandHandler handler = new UpdateIngredientCommandHandler();
        UpdateIngredientCommand command = new UpdateIngredientCommand();
        command.id = UUID.randomUUID();
        command.name = "chocolat updated";
        command.unit = IngredientUnit.TEASPOON;

        assertThatExceptionOfType(MissingAggregateRootException.class).isThrownBy(() -> handler.execute(command));
    }
}