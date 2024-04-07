package nimnamfood.command.ingredient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import nimnamfood.model.ingredient.IngredientUnit;
import vtertre.command.Command;

import java.util.UUID;

public class CreateIngredientCommand implements Command<UUID> {
    @NotBlank
    public String name;
    @NotNull
    public IngredientUnit unit;
}
