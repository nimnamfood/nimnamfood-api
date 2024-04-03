package nimnamfood.command;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CreateRecipeCommandHandler implements CommandHandler<CreateRecipeCommand, UUID> {
    @Override
    public Tuple<UUID, List<DomainEvent>> execute(CreateRecipeCommand command) {
        final Set<RecipeIngredient> recipeIngredients = recipeIngredients(command.ingredients);
        final Set<UUID> tagIds = getTagIds(command.tagIds);

        final Recipe recipe = Recipe.factory().create(
                command.name,
                command.portionsCount,
                recipeIngredients,
                command.instructions,
                tagIds
        );
        Repositories.recipes().add(recipe);
        return Tuple.of(recipe.getId(), Collections.emptyList());
    }

    private static Set<RecipeIngredient> recipeIngredients(Set<RecipeIngredientCommandPart> parts) {
        return parts
                .stream()
                .map(part -> {
                    final UUID ingredientId = UUID.fromString(part.ingredientId);

                    if (!Repositories.ingredients().exists(ingredientId)) {
                        throw new MissingAggregateRootException(ingredientId);
                    }

                    return new RecipeIngredient(
                            UUID.fromString(part.ingredientId), part.quantity, part.unit, part.quantityFixed);
                })
                .collect(Collectors.toSet());
    }

    private static Set<UUID> getTagIds(Set<String> tagIds) {
        return tagIds.stream().map(stringUuid -> {
            final UUID tagId = UUID.fromString(stringUuid);

            if (!Repositories.tags().exists(tagId)) {
                throw new MissingAggregateRootException(tagId);
            }

            return tagId;
        }).collect(Collectors.toSet());
    }
}
