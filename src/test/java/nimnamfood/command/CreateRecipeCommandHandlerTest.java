package nimnamfood.command;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.tag.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class CreateRecipeCommandHandlerTest {
    @Test
    void addsTheRecipeToTheRepositoryAndReturnsItsUUID() {
        CreateRecipeCommandHandler handler = new CreateRecipeCommandHandler();

        Tag tag = new Tag("rapide");
        Repositories.tags().add(tag);

        Ingredient ingredient = new Ingredient("cacao", IngredientUnit.GRAM);
        Repositories.ingredients().add(ingredient);
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
            quantityFixed = false;
        }};

        CreateRecipeCommand command = new CreateRecipeCommand();
        command.name = "chocolat";
        command.portionsCount = 1;
        command.instructions = "instructions";
        command.tagIds = Set.of(tag.getId().toString());
        command.ingredients = Set.of(part);

        UUID result = handler.execute(command);
        Recipe recipe = Repositories.recipes().get(result).get();

        assertThat(recipe.getName()).isEqualTo("chocolat");
        assertThat(recipe.getPortionsCount()).isEqualTo(1);
        assertThat(recipe.getInstructions()).isEqualTo("instructions");
        assertThat(recipe.getTagIds()).containsExactly(tag.getId());
        assertThat(recipe.getIngredients()).hasSize(1);
        assertThat(recipe.getIngredients().stream().findFirst().get()).matches(ri -> ri.getId() != null &&
                ri.ingredientId().equals(ingredient.getId()) && ri.quantity() == 20f &&
                ri.unit() == IngredientUnit.PIECE && !ri.quantityFixed());
    }
}
