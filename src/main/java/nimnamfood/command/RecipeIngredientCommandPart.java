package nimnamfood.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import nimnamfood.model.ingredient.IngredientUnit;
import org.hibernate.validator.constraints.UUID;

public class RecipeIngredientCommandPart {
    @NotNull
    @UUID
    public String ingredientId;

    @NotNull
    @Positive
    public Float quantity;

    @NotNull
    public IngredientUnit unit;

    @NotNull
    public Boolean quantityFixed;
}
