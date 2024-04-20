package nimnamfood.model.recipe;

import com.google.common.collect.ImmutableSet;
import vtertre.ddd.event.DomainEvent;

import java.util.UUID;

public record RecipeChanged(
        UUID id,
        String name,
        UUID illustrationId,
        int portionsCount,
        String instructions,
        ImmutableSet<RecipeIngredient> ingredients,
        ImmutableSet<UUID> tagIds
) implements DomainEvent {
}
