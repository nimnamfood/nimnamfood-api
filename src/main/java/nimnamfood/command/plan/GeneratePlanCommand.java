package nimnamfood.command.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import vtertre.command.Command;

import java.util.List;
import java.util.UUID;

public class GeneratePlanCommand implements Command<UUID> {
    @Valid
    public TagFilterCommandPart globalTagFilters = new TagFilterCommandPart();

    @NotNull
    @NotEmpty
    public List<@Valid MealConfigurationCommandPart> meals;
}
