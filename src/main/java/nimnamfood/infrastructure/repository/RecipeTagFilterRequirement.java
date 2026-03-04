package nimnamfood.infrastructure.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record RecipeTagFilterRequirement(Set<UUID> requiredTagsIds, Set<UUID> excludedTagIds,
                                         List<Set<UUID>> oneOfTagsIdsCombinations) {
}
