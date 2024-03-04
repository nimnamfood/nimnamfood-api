package nimnamfood.model;

import nimnamfood.model.ingredient.IngredientRepository;
import nimnamfood.model.tag.TagRepository;
import org.springframework.beans.factory.InitializingBean;

public abstract class Repositories implements InitializingBean {
    private static Repositories INSTANCE;

    protected abstract TagRepository getTags();
    protected abstract IngredientRepository getIngredients();

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
}
