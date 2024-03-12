package nimnamfood.model.recipe;

import nimnamfood.model.tag.Tag;
import vtertre.ddd.BaseAggregateRootWithUuid;

import java.util.List;

public class Recipe extends BaseAggregateRootWithUuid {
    private final String name;
    private final int portionsCount;
    private final List<RecipeIngredient> ingredients;
    private final String instructions;
    private final List<Tag> tags;

    public static Factory factory() {
        return new Factory();
    }

    private Recipe(String name, int portionsCount, List<RecipeIngredient> ingredients, String instructions, List<Tag> tags) {
        this.name = name;
        this.portionsCount = portionsCount;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.tags = tags;
    }

    public static class Factory {
        public Recipe create(String name, int portionsCount, List<RecipeIngredient> ingredients, String instructions, List<Tag> tags) {
            return new Recipe(name, portionsCount, ingredients, instructions, tags);
        }
    }

    public String getName() {
        return this.name;
    }

    public int getPortionsCount() {
        return this.portionsCount;
    }

    public List<RecipeIngredient> getIngredients() {
        return this.ingredients;
    }

    public String getInstructions() {
        return this.instructions;
    }

    public List<Tag> getTags() {
        return this.tags;
    }
}
