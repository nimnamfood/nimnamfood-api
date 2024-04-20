package nimnamfood.command.recipe;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeCreated;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import nimnamfood.service.RecipeService;
import org.assertj.core.api.InstanceOfAssertFactories;
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

@ExtendWith(WithMemoryRepositories.class)
public class CreateRecipeCommandHandlerTest {
    RecipeService recipeService = Mockito.mock();

    @Test
    void addsTheRecipeToTheRepositoryAndReturnsItsUUID() {
        CreateRecipeCommandHandler handler = new CreateRecipeCommandHandler(recipeService);

        Tag tag = Tag.factory().create("rapide")._1;
        Repositories.tags().add(tag);

        Ingredient ingredient = Ingredient.factory().create("cacao", IngredientUnit.GRAM)._1;
        Repositories.ingredients().add(ingredient);
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
            quantityFixed = false;
        }};

        UUID illustrationId = UUID.randomUUID();
        CreateRecipeCommand command = new CreateRecipeCommand();
        command.name = "chocolat";
        command.illustrationId = illustrationId.toString();
        command.portionsCount = 1;
        command.instructions = "instructions";
        command.tagIds = Set.of(tag.getId().toString());
        command.ingredients = Set.of(part);

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);
        Recipe recipe = Repositories.recipes().get(result._1).get();

        assertThat(recipe.getName()).isEqualTo("chocolat");
        assertThat(recipe.getIllustrationId()).isEqualTo(illustrationId);
        assertThat(recipe.getPortionsCount()).isEqualTo(1);
        assertThat(recipe.getInstructions()).isEqualTo("instructions");
        assertThat(recipe.getTagIds()).containsExactly(tag.getId());
        assertThat(recipe.getIngredients()).hasSize(1);
        assertThat(recipe.getIngredients().stream().findFirst().get()).matches(ri -> ri.getId() != null &&
                ri.ingredientId().equals(ingredient.getId()) && ri.quantity() == 20f &&
                ri.unit() == IngredientUnit.PIECE && !ri.quantityFixed());
    }

    @Test
    void generatesTheDomainEvent() {
        CreateRecipeCommandHandler handler = new CreateRecipeCommandHandler(recipeService);
        CreateRecipeCommand command = createDefaultCommand();

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);
        Recipe recipe = Repositories.recipes().get(result._1).get();

        assertThat(result._2).hasSize(1).first()
                .asInstanceOf(InstanceOfAssertFactories.type(RecipeCreated.class))
                .satisfies(event -> {
                    assertThat(event.id()).isEqualTo(result._1);
                    assertThat(event.name()).isEqualTo("chocolat");
                    assertThat(event.illustrationId()).isNull();
                    assertThat(event.portionsCount()).isEqualTo(1);
                    assertThat(event.instructions()).isEqualTo("instructions");
                    assertThat(event.ingredients()).containsExactly(recipe.getIngredients().toArray(new RecipeIngredient[0]));
                    assertThat(event.tagIds()).isEmpty();
                    assertThat(event.creationDateTime()).isEqualTo(recipe.getCreationDateTime());
                });
    }

    @Test
    void illustrationIsOptional() {
        CreateRecipeCommandHandler handler = new CreateRecipeCommandHandler(recipeService);
        CreateRecipeCommand command = createDefaultCommand();

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);
        Recipe recipe = Repositories.recipes().get(result._1).get();

        assertThat(recipe.getName()).isEqualTo("chocolat");
        assertThat(recipe.getIllustrationId()).isNull();
        Mockito.verify(recipeService, Mockito.never())
                .activateIllustration(Mockito.any());
    }

    @Test
    void activatesTheRecipeIllustration() {
        CreateRecipeCommandHandler handler = new CreateRecipeCommandHandler(recipeService);
        UUID illustrationId = UUID.randomUUID();
        CreateRecipeCommand command = createDefaultCommand();
        command.illustrationId = illustrationId.toString();

        handler.execute(command);

        Mockito.verify(recipeService, Mockito.times(1))
                .activateIllustration(illustrationId);
    }

    @Test
    void throwsAndExceptionIfAnIngredientDoesNotExist() {
        CreateRecipeCommandHandler handler = new CreateRecipeCommandHandler(recipeService);

        Ingredient ingredient = Ingredient.factory().create("cacao", IngredientUnit.GRAM)._1;
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
            quantityFixed = false;
        }};

        CreateRecipeCommand command = new CreateRecipeCommand();
        command.ingredients = Set.of(part);

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(command))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + ingredient.getId().toString());
    }

    @Test
    void throwsAndExceptionIfATagDoesNotExist() {
        CreateRecipeCommandHandler handler = new CreateRecipeCommandHandler(recipeService);
        Tag tag = Tag.factory().create("rapide")._1;
        RecipeIngredientCommandPart part = createDefaultRecipeIngredientCommandPart();
        CreateRecipeCommand command = new CreateRecipeCommand();
        command.ingredients = Set.of(part);
        command.tagIds = Set.of(tag.getId().toString());

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> handler.execute(command))
                .withMessage("AGGREGATE_ROOT_NOT_FOUND - " + tag.getId().toString());
    }

    private static RecipeIngredientCommandPart createDefaultRecipeIngredientCommandPart() {
        Ingredient ingredient = Ingredient.factory().create("cacao", IngredientUnit.GRAM)._1;
        Repositories.ingredients().add(ingredient);
        return new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
            quantityFixed = false;
        }};
    }

    private static CreateRecipeCommand createDefaultCommand() {
        RecipeIngredientCommandPart part = createDefaultRecipeIngredientCommandPart();

        CreateRecipeCommand command = new CreateRecipeCommand();
        command.name = "chocolat";
        command.portionsCount = 1;
        command.instructions = "instructions";
        command.tagIds = Collections.emptySet();
        command.ingredients = Set.of(part);
        return command;
    }
}
