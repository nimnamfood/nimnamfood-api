package nimnamfood.model.recipe;

import vtertre.ddd.BaseAggregateRootWithUuid;

import java.util.Set;
import java.util.UUID;

public class Recipe extends BaseAggregateRootWithUuid {
    private final String name;
    private final int portionsCount;
    private final String instructions;
    private final Set<RecipeIngredient> ingredients;
    private final Set<UUID> tagIds;

    public static Factory factory() {
        return new Factory();
    }

    private Recipe(String name, int portionsCount, Set<RecipeIngredient> ingredients, String instructions, Set<UUID> tagIds) {
        this.name = name;
        this.portionsCount = portionsCount;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.tagIds = tagIds;
    }

    public static class Factory {
        public Recipe create(String name, int portionsCount, Set<RecipeIngredient> ingredients,
                             String instructions, Set<UUID> tagIds) {
            return new Recipe(name, portionsCount, ingredients, instructions, tagIds);
        }
    }

    public String getName() {
        return this.name;
    }

    public int getPortionsCount() {
        return this.portionsCount;
    }

    public Set<RecipeIngredient> getIngredients() {
        return this.ingredients;
    }

    public String getInstructions() {
        return this.instructions;
    }

    public Set<UUID> getTagIds() {
        return this.tagIds;
    }
}
