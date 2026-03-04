package nimnamfood.model.recipe;

import nimnamfood.infrastructure.repository.RecipeTagFilterRequirement;
import vtertre.ddd.RepositoryWithUuid;

import java.util.Set;
import java.util.UUID;

public interface RecipeRepository extends RepositoryWithUuid<Recipe> {
    Iterable<Recipe> getAllById(Set<UUID> recipeIds);

    Set<UUID> findIdsByTagFilterRequirement(RecipeTagFilterRequirement requirement);
}
