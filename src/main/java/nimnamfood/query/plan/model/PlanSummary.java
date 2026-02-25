package nimnamfood.query.plan.model;

import java.util.Set;
import java.util.UUID;

public record PlanSummary(UUID id, Set<MealSummary> meals) {
}
