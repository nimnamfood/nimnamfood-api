package nimnamfood.command.recipe;

import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.service.RecipeService;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class CreateRecipeCommandHandler implements CommandHandler<CreateRecipeCommand, UUID> {
    private final RecipeService recipeService;

    public CreateRecipeCommandHandler(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Override
    public Tuple<UUID, List<DomainEvent>> execute(CreateRecipeCommand command) {
        final Set<RecipeIngredient> recipeIngredients = RecipeService.recipeIngredientsFromCommand(command.ingredients);
        final Set<UUID> tagIds = RecipeService.tagIdsFromCommand(command.tagIds);
        final UUID illustrationId = command.illustrationId != null ? UUID.fromString(command.illustrationId) : null;

        if (illustrationId != null) {
            this.recipeService.activateIllustration(illustrationId);
        }

        final Recipe recipe = Recipe.factory().create(
                command.name,
                illustrationId,
                command.portionsCount,
                recipeIngredients,
                command.instructions,
                tagIds
        );
        Repositories.recipes().add(recipe);
        return Tuple.of(recipe.getId(), Collections.emptyList());
    }
}
