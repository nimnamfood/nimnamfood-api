package nimnamfood.infrastructure.repository.jdbc.plan;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("meals")
public class MealDbo {
    @Id
    UUID id;
    Integer mealIndex;
    UUID recipeId;

    public UUID id() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer mealIndex() {
        return mealIndex;
    }

    public void setMealIndex(Integer mealIndex) {
        this.mealIndex = mealIndex;
    }

    public UUID recipeId() {
        return recipeId;
    }

    public void setRecipeId(UUID recipeId) {
        this.recipeId = recipeId;
    }
}
