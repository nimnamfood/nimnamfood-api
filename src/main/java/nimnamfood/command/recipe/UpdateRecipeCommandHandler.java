package nimnamfood.command.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.*;

@Component
public class UpdateRecipeCommandHandler implements CommandHandler<UpdateRecipeCommand, Void> {
    private final RecipeService recipeService;

    @Autowired
    public UpdateRecipeCommandHandler(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Override
    public Tuple<Void, List<DomainEvent>> execute(UpdateRecipeCommand command) {
        final Optional<Recipe> currentRecipe = Repositories.recipes().get(command.id);
        if (currentRecipe.isEmpty()) {
            throw new MissingAggregateRootException(command.id);
        }

        final Set<RecipeIngredient> recipeIngredients = RecipeService.recipeIngredientsFromCommand(command.ingredients);
        final Set<UUID> tagIds = RecipeService.tagIdsFromCommand(command.tagIds);
        final UUID newIllustrationId = command.illustrationId != null ? UUID.fromString(command.illustrationId) : null;
        final UUID currentIllustrationId = currentRecipe.get().getIllustrationId();

        if (newIllustrationId != null && !newIllustrationId.equals(currentIllustrationId)) {
            this.activateNewIllustration(currentIllustrationId, newIllustrationId);
        } else if (currentIllustrationId != null) {
            this.recipeService.deleteIllustration(currentIllustrationId);
        }

        final Recipe recipe = Recipe.factory().create(
                command.name,
                newIllustrationId,
                command.portionsCount,
                recipeIngredients,
                command.instructions,
                tagIds
        );
        recipe.setId(command.id);
        Repositories.recipes().update(recipe);

        return Tuple.of(null, Collections.emptyList());
    }

    private void activateNewIllustration(UUID currentIllustrationId, UUID newIllustrationId) {
        if (currentIllustrationId == null) {
            this.recipeService.activateIllustration(newIllustrationId);
            return;
        }

        this.recipeService.replaceIllustration(currentIllustrationId, newIllustrationId);
    }
}
