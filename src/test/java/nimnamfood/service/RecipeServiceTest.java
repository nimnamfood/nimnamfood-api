package nimnamfood.service;

import nimnamfood.command.recipe.RecipeIngredientCommandPart;
import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.MissingAggregateRootException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith({WithMemoryRepositories.class})
class RecipeServiceTest {
    @Test
    void extractsTheSetOfRecipeIngredientsFromACommandPart() {
        Ingredient ingredient = addIngredient(new Ingredient("ingredient", IngredientUnit.GRAM));
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 2f;
            unit = IngredientUnit.GRAM;
            quantityFixed = false;
        }};

        Set<RecipeIngredient> result = RecipeService.recipeIngredientsFromCommand(Set.of(part));

        assertThat(result)
                .hasSize(1)
                .first()
                .matches(ri -> ri.getId() != null && ri.ingredientId().equals(ingredient.getId()) &&
                        ri.quantity() == 2f && ri.unit() == IngredientUnit.GRAM && !ri.quantityFixed());
    }

    @Test
    void throwsAnExceptionIfAnIngredientDoesNotExist() {
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = UUID.randomUUID().toString();
            quantity = 2f;
            unit = IngredientUnit.GRAM;
            quantityFixed = false;
        }};

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> RecipeService.recipeIngredientsFromCommand(Set.of(part)));
    }

    @Test
    void extractsTheSetOfTagAsUuids() {
        Tag tag1 = addTag(new Tag("1"));
        Tag tag2 = addTag(new Tag("2"));

        Set<UUID> result = RecipeService.tagIdsFromCommand(Set.of(tag1.getId().toString(), tag2.getId().toString()));

        assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(tag1.getId(), tag2.getId());
    }

    @Test
    void throwsAnExceptionIfATagDoesNotExist() {
        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> RecipeService.tagIdsFromCommand(Set.of(UUID.randomUUID().toString())));
    }

    private static Ingredient addIngredient(Ingredient ingredient) {
        Repositories.ingredients().add(ingredient);
        return ingredient;
    }

    private static Tag addTag(Tag tag) {
        Repositories.tags().add(tag);
        return tag;
    }
}