package nimnamfood.infrastructure.repository.jdbc.recipe;

import com.google.common.collect.Sets;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import vtertre.infrastructure.persistence.jdbc.BaseJdbcDboWithUuid;

import java.util.Set;
import java.util.stream.Collectors;

@Table("recipes")
public class RecipeDbo extends BaseJdbcDboWithUuid<Recipe> {
    String name;
    Integer portionsCount;
    String instructions;

    @MappedCollection(idColumn = "recipe_id")
    Set<RecipeIngredientDbo> ingredients = Sets.newHashSet();
    @MappedCollection(idColumn = "recipe_id")
    Set<RecipeTagDbo> tags = Sets.newHashSet();

    @Override
    public Recipe asAggregateRoot() {
        Set<RecipeIngredient> recipeIngredients = this.ingredients.stream().map(recipeIngredientDbo ->
                new RecipeIngredient(recipeIngredientDbo.ingredientId, recipeIngredientDbo.quantity,
                        recipeIngredientDbo.unit, recipeIngredientDbo.quantityFixed)).collect(Collectors.toSet());
        final Recipe recipe = Recipe.factory().create(
                this.name, this.portionsCount, recipeIngredients, this.instructions,
                this.tags.stream().map(tag -> tag.tagId).collect(Collectors.toSet()));
        recipe.setId(this.getId());
        return recipe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPortionsCount() {
        return portionsCount;
    }

    public void setPortionsCount(Integer portionsCount) {
        this.portionsCount = portionsCount;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Set<RecipeTagDbo> getTags() {
        return tags;
    }

    public void setTags(Set<RecipeTagDbo> tags) {
        this.tags = tags;
    }

    public Set<RecipeIngredientDbo> getIngredients() {
        return ingredients;
    }

    public void setIngredients(Set<RecipeIngredientDbo> ingredients) {
        this.ingredients = ingredients;
    }
}
