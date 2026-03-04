package nimnamfood.infrastructure.repository.memory;

import nimnamfood.infrastructure.repository.RecipeTagFilterRequirement;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeRepository;
import vtertre.infrastructure.persistence.memory.MemoryRepositoryWithUuid;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RecipeMemoryRepository extends MemoryRepositoryWithUuid<Recipe> implements RecipeRepository {
    @Override
    public Iterable<Recipe> getAllById(Set<UUID> recipeIds) {
        return this.entities.stream().filter(recipe -> recipeIds.contains(recipe.getId())).toList();
    }

    @Override
    public Set<UUID> findIdsByTagFilterRequirement(RecipeTagFilterRequirement requirement) {
        return this.entities.stream()
                .filter(recipe -> {
                    final var recipeTags = recipe.getTagIds();

                    if (!requirement.requiredTagsIds().isEmpty() && !recipeTags.containsAll(requirement.requiredTagsIds())) {
                        return false;
                    }

                    if (!requirement.excludedTagIds().isEmpty() && requirement.excludedTagIds().stream().anyMatch(recipeTags::contains)) {
                        return false;
                    }

                    for (var group : requirement.oneOfTagsIdsCombinations()) {
                        if (group.stream().noneMatch(recipeTags::contains)) {
                            return false;
                        }
                    }

                    return true;
                })
                .map(Recipe::getId)
                .collect(Collectors.toSet());
    }
}
