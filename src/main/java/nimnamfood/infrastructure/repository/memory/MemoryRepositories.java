package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.IngredientRepository;
import nimnamfood.model.plan.PlanRepository;
import nimnamfood.model.recipe.RecipeRepository;
import nimnamfood.model.tag.TagRepository;

public class MemoryRepositories extends Repositories {
    private final TagMemoryRepository tagMemoryRepository = new TagMemoryRepository();
    private final IngredientMemoryRepository ingredientMemoryRepository = new IngredientMemoryRepository();
    private final RecipeMemoryRepository recipeMemoryRepository = new RecipeMemoryRepository();
    private final PlanMemoryRepository planMemoryRepository = new PlanMemoryRepository();

    @Override
    protected TagRepository getTags() {
        return tagMemoryRepository;
    }

    @Override
    protected IngredientRepository getIngredients() {
        return ingredientMemoryRepository;
    }

    @Override
    protected RecipeRepository getRecipes() {
        return recipeMemoryRepository;
    }

    @Override
    protected PlanRepository getPlans() {
        return planMemoryRepository;
    }
}
