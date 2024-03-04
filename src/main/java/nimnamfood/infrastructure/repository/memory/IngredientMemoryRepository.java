package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientRepository;
import vtertre.infrastructure.persistence.memory.MemoryRepositoryWithUuid;

public class IngredientMemoryRepository extends MemoryRepositoryWithUuid<Ingredient> implements IngredientRepository {
}
