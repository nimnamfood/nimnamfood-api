package nimnamfood.command;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;

import java.util.UUID;

@Component
public class CreateIngredientCommandHandler implements CommandHandler<CreateIngredientCommand, UUID> {
    @Override
    public UUID execute(CreateIngredientCommand command) {
        final Ingredient ingredient = new Ingredient(command.name, command.unit);
        Repositories.ingredients().add(ingredient);
        return ingredient.getId();
    }
}
