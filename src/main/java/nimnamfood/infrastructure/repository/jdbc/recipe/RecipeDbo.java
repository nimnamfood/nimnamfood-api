package nimnamfood.infrastructure.repository.jdbc.recipe;

import com.google.common.collect.Sets;
import nimnamfood.model.recipe.Recipe;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import vtertre.infrastructure.persistence.jdbc.BaseJdbcDboWithUuid;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Table("recipes")
public class RecipeDbo extends BaseJdbcDboWithUuid<Recipe> {
    String name;
    UUID illustrationId;
    Integer portionsCount;
    String instructions;
    Instant creationDateTime;

    @MappedCollection(idColumn = "recipe_id")
    Set<RecipeIngredientDbo> ingredients = Sets.newHashSet();
    @MappedCollection(idColumn = "recipe_id")
    Set<RecipeTagDbo> tags = Sets.newHashSet();

    @Override
    public Recipe asAggregateRoot() {
        return Recipe.factory().recreateFromDbo(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getIllustrationId() {
        return illustrationId;
    }

    public void setIllustrationId(UUID illustrationId) {
        this.illustrationId = illustrationId;
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

    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(Instant creationDateTime) {
        this.creationDateTime = creationDateTime;
    }
}
