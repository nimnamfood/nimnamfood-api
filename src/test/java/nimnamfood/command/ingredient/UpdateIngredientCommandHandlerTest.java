package nimnamfood.command.ingredient;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientChanged;
import nimnamfood.model.ingredient.IngredientUnit;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith({WithMemoryRepositories.class})
class UpdateIngredientCommandHandlerTest {
    @Test
    void updatesTheIngredient() {
        UpdateIngredientCommandHandler handler = new UpdateIngredientCommandHandler();
        Ingredient chocolat = Ingredient.factory().create("chocolat", IngredientUnit.GRAM)._1;
        Repositories.ingredients().add(chocolat);

        UpdateIngredientCommand command = new UpdateIngredientCommand();
        command.id = chocolat.getId();
        command.name = "chocolat updated";
        command.unit = IngredientUnit.TEASPOON;

        Tuple<Void, List<DomainEvent>> result = handler.execute(command);
        Ingredient ingredient = Repositories.ingredients().get(chocolat.getId()).get();

        assertThat(result._1).isNull();
        assertThat(ingredient.getName()).isEqualTo("chocolat updated");
        assertThat(ingredient.getUnit()).isEqualTo(IngredientUnit.TEASPOON);
        assertThat(result._2)
                .hasSize(1)
                .first()
                .asInstanceOf(InstanceOfAssertFactories.type(IngredientChanged.class)).satisfies(event -> {
                    assertThat(event.id()).isEqualTo(chocolat.getId());
                    assertThat(event.name()).isEqualTo("chocolat updated");
                    assertThat(event.unit()).isEqualTo(IngredientUnit.TEASPOON);
                });
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