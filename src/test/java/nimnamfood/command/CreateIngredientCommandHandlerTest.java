package nimnamfood.command;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class CreateIngredientCommandHandlerTest {
    @Test
    void addsTheIngredientToTheRepositoryAndReturnsUUID() {
        CreateIngredientCommandHandler handler = new CreateIngredientCommandHandler();
        CreateIngredientCommand command = new CreateIngredientCommand();
        command.name = "chocolat";
        command.unit = IngredientUnit.GRAM;

        UUID result = handler.execute(command);
        Ingredient ingredient = Repositories.ingredients().get(result).get();

        assertThat(ingredient.getId()).isEqualTo(result);
        assertThat(ingredient.getName()).isEqualTo("chocolat");
        assertThat(ingredient.getUnit()).isEqualTo(IngredientUnit.GRAM);
    }
}
