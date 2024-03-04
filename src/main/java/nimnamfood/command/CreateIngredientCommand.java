package nimnamfood.command;

import nimnamfood.model.ingredient.IngredientUnit;
import vtertre.command.Command;

import java.util.UUID;

public class CreateIngredientCommand implements Command<UUID> {
    public String name;
    public IngredientUnit unit;
}
