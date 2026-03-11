package nimnamfood.query.plan.model;

import java.time.Instant;
import java.util.UUID;

public record PlanSearchSummary(UUID id, Instant createdAt) {
}
