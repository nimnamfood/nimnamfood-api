package nimnamfood.model;

import nimnamfood.model.ingredient.IngredientRepository;
import nimnamfood.model.plan.PlanRepository;
import nimnamfood.model.recipe.RecipeRepository;
import nimnamfood.model.tag.TagRepository;
import org.springframework.beans.factory.InitializingBean;

public abstract class Repositories implements InitializingBean {
    private static Repositories INSTANCE;

    protected abstract TagRepository getTags();

    protected abstract IngredientRepository getIngredients();

    protected abstract RecipeRepository getRecipes();

    protected abstract PlanRepository getPlans();

    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    public static void initialize(Repositories instance) {
        Repositories.INSTANCE = instance;
    }

    public static TagRepository tags() {
        return INSTANCE.getTags();
    }

    public static IngredientRepository ingredients() {
        return INSTANCE.getIngredients();
    }

    public static RecipeRepository recipes() {
        return INSTANCE.getRecipes();
    }

    public static PlanRepository plans() {
        return INSTANCE.getPlans();
    }
}
