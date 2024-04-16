package nimnamfood.model.ingredient;

import vtertre.ddd.event.DomainEvent;

import java.util.UUID;

public record IngredientCreated(UUID id, String name, IngredientUnit unit) implements DomainEvent {
}
