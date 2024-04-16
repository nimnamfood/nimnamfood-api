package nimnamfood.command.recipe;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
    RecipeService recipeService = Mockito.mock();

    @Test
    void updatesTheRecipe() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler(recipeService);

        Tag tag = Tag.factory().create("rapide")._1;
        Repositories.tags().add(tag);

        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.GRAM)._1;
        Repositories.ingredients().add(ingredient);

        RecipeIngredient recipeIngredient = new RecipeIngredient(ingredient.getId(), 1f, IngredientUnit.GRAM, false);
        Recipe recipe = Recipe.factory().create("recette", null, 1, Set.of(recipeIngredient), "instructions", Collections.emptySet());
        Repositories.recipes().add(recipe);

        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
            quantityFixed = true;
        }};

        UUID illustrationId = UUID.randomUUID();
        UpdateRecipeCommand command = new UpdateRecipeCommand();
        command.id = recipe.getId();
        command.name = "recette updated";
        command.illustrationId = illustrationId.toString();
        command.portionsCount = 2;
        command.instructions = "instructions updated";
        command.tagIds = Set.of(tag.getId().toString());
        command.ingredients = Set.of(part);

        Tuple<Void, List<DomainEvent>> result = handler.execute(command);
        Recipe updatedRecipe = Repositories.recipes().get(recipe.getId()).get();

        assertThat(updatedRecipe.getName()).isEqualTo("recette updated");
        assertThat(updatedRecipe.getIllustrationId()).isEqualTo(illustrationId);
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
    void preservesTheCreationDate() {
        Recipe recipe = createRecipeWithoutIllustration();

        new UpdateRecipeCommandHandler(recipeService).execute(createDefaultCommand(recipe));
        Recipe updatedRecipe = Repositories.recipes().get(recipe.getId()).get();

        assertThat(updatedRecipe.getCreationDateTime()).isEqualTo(recipe.getCreationDateTime());
    }

    @Test
    void addsTheIllustrationIfNoneExists() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler(recipeService);
        Recipe recipe = createRecipeWithoutIllustration();
        UUID nextIllustrationId = UUID.randomUUID();
        UpdateRecipeCommand command = createDefaultCommand(recipe);
        command.illustrationId = nextIllustrationId.toString();

        handler.execute(command);

        Mockito.verify(recipeService, Mockito.times(1))
                .activateIllustration(nextIllustrationId);
        Mockito.verify(recipeService, Mockito.never())
                .replaceIllustration(Mockito.any(), Mockito.any());
    }

    @Test
    void replacesTheCurrentIllustration() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler(recipeService);
        Recipe recipe = createRecipeWithIllustration();
        UUID nextIllustrationId = UUID.randomUUID();
        UpdateRecipeCommand command = createDefaultCommand(recipe);
        command.illustrationId = nextIllustrationId.toString();

        handler.execute(command);

        Mockito.verify(recipeService, Mockito.times(1))
                .replaceIllustration(recipe.getIllustrationId(), nextIllustrationId);
        Mockito.verify(recipeService, Mockito.never())
                .activateIllustration(Mockito.any());
    }

    @Test
    void canKeepTheCurrentIllustration() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler(recipeService);
        Recipe recipe = createRecipeWithIllustration();
        UpdateRecipeCommand command = createDefaultCommand(recipe);
        command.illustrationId = recipe.getIllustrationId().toString();

        handler.execute(command);

        Mockito.verify(recipeService, Mockito.never())
                .replaceIllustration(Mockito.any(), Mockito.any());
        Mockito.verify(recipeService, Mockito.never())
                .activateIllustration(Mockito.any());
    }

    @Test
    void canDeleteTheCurrentIllustration() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler(recipeService);
        Recipe recipe = createRecipeWithIllustration();
        UpdateRecipeCommand command = createDefaultCommand(recipe);
        command.illustrationId = null;

        handler.execute(command);

        Mockito.verify(recipeService, Mockito.times(1))
                .deleteIllustration(recipe.getIllustrationId());
        Mockito.verify(recipeService, Mockito.never())
                .replaceIllustration(Mockito.any(), Mockito.any());
        Mockito.verify(recipeService, Mockito.never())
                .activateIllustration(Mockito.any());
    }

    @Test
    void throwsAnExceptionIfTheRecipeDoesNotExist() {
        UpdateRecipeCommandHandler handler = new UpdateRecipeCommandHandler(recipeService);
        UpdateRecipeCommand command = new UpdateRecipeCommand();
        command.id = UUID.randomUUID();

        assertThatExceptionOfType(MissingAggregateRootException.class).isThrownBy(() -> handler.execute(command));
    }

    private static Recipe createRecipeWithIllustration() {
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.GRAM)._1;
        Repositories.ingredients().add(ingredient);

        RecipeIngredient recipeIngredient = new RecipeIngredient(ingredient.getId(), 1f, IngredientUnit.GRAM, false);
        Recipe recipe = Recipe.factory().create("recette", UUID.randomUUID(), 1, Set.of(recipeIngredient), "instructions", Collections.emptySet());
        Repositories.recipes().add(recipe);
        return recipe;
    }

    private static Recipe createRecipeWithoutIllustration() {
        Ingredient ingredient = Ingredient.factory().create("ingredient", IngredientUnit.GRAM)._1;
        Repositories.ingredients().add(ingredient);

        RecipeIngredient recipeIngredient = new RecipeIngredient(ingredient.getId(), 1f, IngredientUnit.GRAM, false);
        Recipe recipe = Recipe.factory().create("recette", 1, Set.of(recipeIngredient), "instructions");
        Repositories.recipes().add(recipe);
        return recipe;
    }

    private static UpdateRecipeCommand createDefaultCommand(Recipe recipe) {
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = recipe.getIngredients().stream().findFirst().get().ingredientId().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
            quantityFixed = false;
        }};

        UpdateRecipeCommand command = new UpdateRecipeCommand().withId(recipe.getId());
        command.name = "chocolat";
        command.illustrationId = UUID.randomUUID().toString();
        command.portionsCount = 1;
        command.instructions = "instructions";
        command.tagIds = Collections.emptySet();
        command.ingredients = Set.of(part);
        return command;
    }
}