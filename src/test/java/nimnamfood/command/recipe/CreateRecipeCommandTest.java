package nimnamfood.command.recipe;

import com.google.common.collect.Sets;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import nimnamfood.model.ingredient.IngredientUnit;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateRecipeCommandTest {
    final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    @Test
    void tagsCanBeEmpty() {
        CreateRecipeCommand command = new CreateRecipeCommand();
        command.name = "recipe";
        command.portionsCount = 1;
        command.instructions = "instructions";
        command.tagIds = Collections.emptySet();
        command.ingredients = Sets.newHashSet(createDefaultRecipeIngredientCommandPart());

        Set<ConstraintViolation<CreateRecipeCommand>> violations = factory.getValidator().validate(command);

        assertThat(violations).isEmpty();
    }

    @Test
    void ingredientsCanBeEmpty() {
        CreateRecipeCommand command = new CreateRecipeCommand() {{
            name = "recipe";
            portionsCount = 1;
            instructions = "instructions";
            tagIds = Sets.newHashSet(UUID.randomUUID().toString());
            ingredients = Collections.emptySet();
        }};

        Set<ConstraintViolation<CreateRecipeCommand>> violations = factory.getValidator().validate(command);

        assertThat(violations).isEmpty();
    }

    private static RecipeIngredientCommandPart createDefaultRecipeIngredientCommandPart() {
        return new RecipeIngredientCommandPart() {{
            ingredientId = UUID.randomUUID().toString();
            quantity = 20f;
            unit = IngredientUnit.PIECE;
        }};
    }
}
