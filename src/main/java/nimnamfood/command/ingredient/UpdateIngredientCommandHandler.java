package nimnamfood.command.ingredient;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;

@Component
public class UpdateIngredientCommandHandler implements CommandHandler<UpdateIngredientCommand, Void> {
    @Override
    public Tuple<Void, List<DomainEvent>> execute(UpdateIngredientCommand command) {
        if (!Repositories.ingredients().exists(command.id)) {
            throw new MissingAggregateRootException(command.id);
        }

        final Ingredient updatedIngredient = new Ingredient(command.id, command.name, command.unit);
        Repositories.ingredients().update(updatedIngredient);

        return Tuple.of(null, Collections.emptyList());
    }
}
