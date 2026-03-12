package nimnamfood.query.recipe.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RecipeSearchSummaryInspector(UUID id, String name, String illustrationUrl, Instant creationDateTime,
                                           Set<RecipeTagSummaryPartInspector> tags) {
}
