package nimnamfood.command;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;

import java.util.List;
import java.util.UUID;

@Component
public class CreateRecipeCommandHandler implements CommandHandler<CreateRecipeCommand, UUID> {
    @Override
    public UUID execute(CreateRecipeCommand command) {
        final List<RecipeIngredient> recipeIngredients = command.ingredients
                .stream()
                .map(part -> new RecipeIngredient(UUID.fromString(part.ingredientId), part.quantity, part.quantityFixed))
                .toList();

        final Recipe recipe = Recipe.factory().create(
                command.name,
                command.portionsCount,
                recipeIngredients,
                command.instructions,
                command.tagIds.stream().map(UUID::fromString).toList()
        );
        Repositories.recipes().add(recipe);
        return recipe.getId();
    }
}
