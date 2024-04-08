package nimnamfood.command.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.service.RecipeService;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class UpdateRecipeCommandHandler implements CommandHandler<UpdateRecipeCommand, Void> {
    @Override
    public Tuple<Void, List<DomainEvent>> execute(UpdateRecipeCommand command) {
        if (!Repositories.recipes().exists(command.id)) {
            throw new MissingAggregateRootException(command.id);
        }

        final Set<RecipeIngredient> recipeIngredients = RecipeService.recipeIngredientsFromCommand(command.ingredients);
        final Set<UUID> tagIds = RecipeService.tagIdsFromCommand(command.tagIds);

        final Recipe recipe = Recipe.factory().create(
                command.name,
                command.portionsCount,
                recipeIngredients,
                command.instructions,
                tagIds
        );
        recipe.setId(command.id);
        Repositories.recipes().update(recipe);

        return Tuple.of(null, Collections.emptyList());
    }
}
