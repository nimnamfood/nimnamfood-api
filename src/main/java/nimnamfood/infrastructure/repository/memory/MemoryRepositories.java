package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.ingredient.IngredientRepository;
import nimnamfood.model.tag.TagRepository;
import nimnamfood.model.Repositories;

public class MemoryRepositories extends Repositories {
    private final TagMemoryRepository tagMemoryRepository = new TagMemoryRepository();
    private final IngredientMemoryRepository ingredientMemoryRepository = new IngredientMemoryRepository();

    @Override
    protected TagRepository getTags() {
        return tagMemoryRepository;
    }

    @Override
    protected IngredientRepository getIngredients() {
        return ingredientMemoryRepository;
    }
}
