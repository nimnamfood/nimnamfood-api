package nimnamfood.command.ingredient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import nimnamfood.model.ingredient.IngredientUnit;
import vtertre.command.Command;

import java.util.UUID;

public class UpdateIngredientCommand implements Command<Void> {
    @NotNull
    public UUID id;
    @NotBlank
    public String name;
    @NotNull
    public IngredientUnit unit;

    public UpdateIngredientCommand withId(UUID id) {
        this.id = id;
        return this;
    }
}
