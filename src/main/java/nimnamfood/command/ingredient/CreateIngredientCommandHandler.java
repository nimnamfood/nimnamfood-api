package nimnamfood.command.ingredient;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class CreateIngredientCommandHandler implements CommandHandler<CreateIngredientCommand, UUID> {
    @Override
    public Tuple<UUID, List<DomainEvent>> execute(CreateIngredientCommand command) {
        final Ingredient ingredient = new Ingredient(command.name, command.unit);
        Repositories.ingredients().add(ingredient);
        return Tuple.of(ingredient.getId(), Collections.emptyList());
    }
}
