package nimnamfood.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UUID;

public class RecipeIngredientCommandPart {
    @NotNull
    @UUID
    public String ingredientId;

    @NotNull
    @Positive
    public Float quantity;

    @NotNull
    public Boolean quantityFixed;
}
