package nimnamfood.command;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CreateRecipeCommandHandler implements CommandHandler<CreateRecipeCommand, UUID> {
    @Override
    public UUID execute(CreateRecipeCommand command) {
        final Set<RecipeIngredient> recipeIngredients = command.ingredients
                .stream()
                .map(part -> new RecipeIngredient(
                        UUID.fromString(part.ingredientId), part.quantity, part.unit, part.quantityFixed))
                .collect(Collectors.toSet());
        ;

        final Recipe recipe = Recipe.factory().create(
                command.name,
                command.portionsCount,
                recipeIngredients,
                command.instructions,
                command.tagIds.stream().map(UUID::fromString).collect(Collectors.toSet())
        );
        Repositories.recipes().add(recipe);
        return recipe.getId();
    }
}
