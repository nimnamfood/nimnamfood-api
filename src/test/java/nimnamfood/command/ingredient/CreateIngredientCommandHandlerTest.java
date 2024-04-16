package nimnamfood.command.ingredient;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientCreated;
import nimnamfood.model.ingredient.IngredientUnit;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
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

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);
        Ingredient ingredient = Repositories.ingredients().get(result._1).get();

        assertThat(ingredient.getId()).isEqualTo(result._1);
        assertThat(ingredient.getName()).isEqualTo("chocolat");
        assertThat(ingredient.getUnit()).isEqualTo(IngredientUnit.GRAM);
        assertThat(result._2).hasSize(1).first()
                .asInstanceOf(InstanceOfAssertFactories.type(IngredientCreated.class))
                .satisfies(event -> {
                    assertThat(event.id()).isEqualTo(result._1);
                    assertThat(event.name()).isEqualTo("chocolat");
                    assertThat(event.unit()).isEqualTo(IngredientUnit.GRAM);
                });
    }
}
