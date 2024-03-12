package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeRepository;
import vtertre.infrastructure.persistence.memory.MemoryRepositoryWithUuid;

public class RecipeMemoryRepository extends MemoryRepositoryWithUuid<Recipe> implements RecipeRepository {
}
