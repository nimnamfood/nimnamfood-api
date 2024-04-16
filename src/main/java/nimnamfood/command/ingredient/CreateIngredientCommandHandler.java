package nimnamfood.command.ingredient;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientCreated;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.UUID;

@Component
public class CreateIngredientCommandHandler implements CommandHandler<CreateIngredientCommand, UUID> {
    @Override
    public Tuple<UUID, List<DomainEvent>> execute(CreateIngredientCommand command) {
        final Tuple<Ingredient, IngredientCreated> tuple = Ingredient.factory().create(command.name, command.unit);
        Repositories.ingredients().add(tuple._1);
        return tuple.map(((ingredient, event) -> Tuple.of(ingredient.getId(), List.of(event))));
    }
}
