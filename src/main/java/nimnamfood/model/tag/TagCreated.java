package nimnamfood.model.tag;

import vtertre.ddd.event.DomainEvent;

import java.util.UUID;

public record TagCreated(UUID id, String name) implements DomainEvent {
}
