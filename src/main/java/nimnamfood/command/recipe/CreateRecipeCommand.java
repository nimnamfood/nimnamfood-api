package nimnamfood.command.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UUID;
import vtertre.command.Command;

import java.util.Set;

public class CreateRecipeCommand implements Command<java.util.UUID> {
    @NotBlank
    public String name;

    @NotNull
    @Positive
    public Integer portionsCount;

    @NotBlank
    public String instructions;

    @NotNull
    public Set<@NotNull @UUID(version = 4) String> tagIds;

    @NotEmpty
    public Set<@Valid RecipeIngredientCommandPart> ingredients;
}
