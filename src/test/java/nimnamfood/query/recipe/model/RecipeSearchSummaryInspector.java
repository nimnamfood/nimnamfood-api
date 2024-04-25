package nimnamfood.query.recipe.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record RecipeSearchSummaryInspector(UUID id, String name, String illustrationUrl, LocalDateTime creationDateTime,
                                           Set<RecipeTagSummaryPartInspector> tags) {
}
