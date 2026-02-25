package nimnamfood.model.plan;

import com.google.common.collect.ImmutableSet;
import vtertre.ddd.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanCreated(UUID id, LocalDateTime createdAt, ImmutableSet<Meal> meals) implements DomainEvent {
}
