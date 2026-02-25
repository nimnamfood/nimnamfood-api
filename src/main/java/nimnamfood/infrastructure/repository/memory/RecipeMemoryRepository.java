package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeRepository;
import vtertre.infrastructure.persistence.memory.MemoryRepositoryWithUuid;

import java.util.Set;
import java.util.UUID;

public class RecipeMemoryRepository extends MemoryRepositoryWithUuid<Recipe> implements RecipeRepository {
    @Override
    public Iterable<Recipe> getAllById(Set<UUID> recipeIds) {
        return this.entities.stream().filter(recipe -> recipeIds.contains(recipe.getId())).toList();
    }
}
