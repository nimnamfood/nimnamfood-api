package nimnamfood.command.recipe;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith({WithMemoryRepositories.class})
class UpdateRecipeCommandHandlerTest {

    @Test
    void updatesTheRecipe() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler();

        Tag tag = new Tag("rapide");
        Repositories.tags().add(tag);

        Ingredient ingredient = new Ingredient("ingredient", IngredientUnit.GRAM);
        Repositories.ingredients().add(ingredient);

        RecipeIngredient recipeIngredient = new RecipeIngredient(ingredient.getId(), 1f, IngredientUnit.GRAM, false);
        Recipe recipe = Recipe.factory().create("recette", 1, Set.of(recipeIngredient), "instructions", Collections.emptySet());
        Repositories.recipes().add(recipe);

        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
            quantityFixed = true;
        }};

        UpdateRecipeCommand command = new UpdateRecipeCommand();
        command.id = recipe.getId();
        command.name = "recette updated";
        command.portionsCount = 2;
        command.instructions = "instructions updated";
        command.tagIds = Set.of(tag.getId().toString());
        command.ingredients = Set.of(part);

        Tuple<Void, List<DomainEvent>> result = handler.execute(command);
        Recipe updatedRecipe = Repositories.recipes().get(recipe.getId()).get();

        assertThat(updatedRecipe.getName()).isEqualTo("recette updated");
        assertThat(updatedRecipe.getPortionsCount()).isEqualTo(2);
        assertThat(updatedRecipe.getInstructions()).isEqualTo("instructions updated");
        assertThat(updatedRecipe.getTagIds()).hasSize(1).first().isEqualTo(tag.getId());
        assertThat(updatedRecipe.getIngredients())
                .hasSize(1)
                .first()
                // RecipeIngredient local id is only used as a way to identify
                // multiple times the same ingredient on a recipe.
                // Updating the ingredients will generate new ids but we can
                // safely ignore it.
                .matches(ri -> ri.ingredientId().equals(ingredient.getId()) && ri.quantity() == 20f &&
                        ri.unit() == IngredientUnit.PIECE && ri.quantityFixed());

        assertThat(result._2).isEmpty();
    }

    @Test
    void throwsAnExceptionIfTheRecipeDoesNotExist() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler();
        UpdateRecipeCommand command = new UpdateRecipeCommand();
        command.id = UUID.randomUUID();

        assertThatExceptionOfType(MissingAggregateRootException.class).isThrownBy(() -> handler.execute(command));
    }
}