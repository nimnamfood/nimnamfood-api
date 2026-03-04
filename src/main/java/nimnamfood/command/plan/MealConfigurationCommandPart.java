package nimnamfood.command.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class MealConfigurationCommandPart {
    @NotNull
    @PositiveOrZero
    public Integer mealIndex;

    @Valid
    public TagFilterCommandPart tagFilters = new TagFilterCommandPart();
}
