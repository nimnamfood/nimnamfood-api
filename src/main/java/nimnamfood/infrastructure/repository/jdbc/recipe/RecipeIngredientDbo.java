package nimnamfood.infrastructure.repository.jdbc.recipe;

import nimnamfood.model.ingredient.IngredientUnit;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("recipe_ingredients")
public class RecipeIngredientDbo {
    @Id
    UUID ingredientId;
    Float quantity;
    IngredientUnit unit;
    Boolean quantityFixed;

    public UUID getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(UUID ingredientId) {
        this.ingredientId = ingredientId;
    }

    public Float getQuantity() {
        return quantity;
    }

    public void setQuantity(Float quantity) {
        this.quantity = quantity;
    }

    public IngredientUnit getUnit() {
        return unit;
    }

    public void setUnit(IngredientUnit unit) {
        this.unit = unit;
    }

    public Boolean getQuantityFixed() {
        return quantityFixed;
    }

    public void setQuantityFixed(Boolean quantityFixed) {
        this.quantityFixed = quantityFixed;
    }
}
