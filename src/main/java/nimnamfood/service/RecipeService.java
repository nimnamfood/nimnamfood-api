package nimnamfood.service;

import nimnamfood.command.recipe.RecipeIngredientCommandPart;
import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.RecipeIngredient;
import org.springframework.stereotype.Service;
import vtertre.ddd.MissingAggregateRootException;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    public static Set<RecipeIngredient> recipeIngredientsFromCommand(Set<RecipeIngredientCommandPart> parts) {
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

    public static Set<UUID> tagIdsFromCommand(Set<String> tagIds) {
        return tagIds.stream().map(stringUuid -> {
            final UUID tagId = UUID.fromString(stringUuid);

            if (!Repositories.tags().exists(tagId)) {
                throw new MissingAggregateRootException(tagId);
            }

            return tagId;
        }).collect(Collectors.toSet());
    }
}
