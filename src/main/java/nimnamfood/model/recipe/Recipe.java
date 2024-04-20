package nimnamfood.model.recipe;

import com.google.common.collect.ImmutableSet;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeDbo;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeTagDbo;
import vtertre.ddd.BaseAggregateRootWithUuid;
import vtertre.ddd.Tuple;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Recipe extends BaseAggregateRootWithUuid {
    private final String name;
    private final UUID illustrationId;
    private final int portionsCount;
    private final String instructions;
    private final Set<RecipeIngredient> ingredients;
    private final Set<UUID> tagIds;
    private final LocalDateTime creationDateTime;

    public static Factory factory() {
        return new Factory();
    }

    private Recipe(String name, UUID illustrationId, int portionsCount, Set<RecipeIngredient> ingredients,
                   String instructions, Set<UUID> tagIds) {
        this.name = name;
        this.illustrationId = illustrationId;
        this.portionsCount = portionsCount;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.tagIds = tagIds;
        this.creationDateTime = LocalDateTime.now(ZoneOffset.UTC);
    }

    private Recipe(UUID id, String name, UUID illustrationId, int portionsCount, Set<RecipeIngredient> ingredients,
                   String instructions, Set<UUID> tagIds, LocalDateTime creationDateTime) {
        super(id);
        this.name = name;
        this.illustrationId = illustrationId;
        this.portionsCount = portionsCount;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.tagIds = tagIds;
        this.creationDateTime = creationDateTime;
    }

    public static class Factory {
        public Tuple<Recipe, RecipeCreated> create(String name, UUID illustrationId, int portionsCount,
                                                   Set<RecipeIngredient> ingredients, String instructions, Set<UUID> tagIds) {
            final Recipe recipe = new Recipe(name, illustrationId, portionsCount, ingredients, instructions, tagIds);
            return Tuple.of(recipe, new RecipeCreated(
                    recipe.getId(),
                    recipe.name,
                    recipe.illustrationId,
                    recipe.portionsCount,
                    recipe.instructions,
                    ImmutableSet.copyOf(recipe.ingredients),
                    ImmutableSet.copyOf(recipe.tagIds),
                    recipe.creationDateTime
            ));
        }

        public Tuple<Recipe, RecipeCreated> create(String name, int portionsCount, Set<RecipeIngredient> ingredients, String instructions,
                                                   Set<UUID> tagIds) {
            return this.create(name, null, portionsCount, ingredients, instructions, tagIds);
        }

        public Tuple<Recipe, RecipeCreated> create(String name, int portionsCount, Set<RecipeIngredient> ingredients, String instructions) {
            return this.create(name, null, portionsCount, ingredients, instructions, Collections.emptySet());
        }

        public Recipe recreateFromDbo(RecipeDbo dbo) {
            final Set<RecipeIngredient> recipeIngredients = dbo.getIngredients().stream().map(recipeIngredientDbo ->
                    new RecipeIngredient(recipeIngredientDbo.getId(), recipeIngredientDbo.getIngredientId(),
                            recipeIngredientDbo.getQuantity(), recipeIngredientDbo.getUnit(),
                            recipeIngredientDbo.getQuantityFixed())).collect(Collectors.toSet());

            final Set<UUID> tagIds = dbo.getTags().stream().map(RecipeTagDbo::getTagId).collect(Collectors.toSet());

            return new Recipe(dbo.getId(), dbo.getName(), dbo.getIllustrationId(), dbo.getPortionsCount(),
                    recipeIngredients, dbo.getInstructions(), tagIds, dbo.getCreationDateTime());
        }
    }

    public Tuple<Recipe, RecipeChanged> updated(String name, UUID illustrationId, int portionsCount,
                                                Set<RecipeIngredient> ingredients, String instructions, Set<UUID> tagIds) {
        final Recipe updatedRecipe = new Recipe(this.getId(), name, illustrationId, portionsCount, ingredients,
                instructions, tagIds, this.creationDateTime);
        return Tuple.of(updatedRecipe, new RecipeChanged(
                this.getId(),
                updatedRecipe.name,
                updatedRecipe.illustrationId,
                updatedRecipe.portionsCount,
                updatedRecipe.instructions,
                ImmutableSet.copyOf(updatedRecipe.ingredients),
                ImmutableSet.copyOf(updatedRecipe.tagIds)
        ));
    }

    public String getName() {
        return this.name;
    }

    public UUID getIllustrationId() {
        return this.illustrationId;
    }

    public int getPortionsCount() {
        return this.portionsCount;
    }

    public Set<RecipeIngredient> getIngredients() {
        return this.ingredients;
    }

    public String getInstructions() {
        return this.instructions;
    }

    public Set<UUID> getTagIds() {
        return this.tagIds;
    }

    public LocalDateTime getCreationDateTime() {
        return this.creationDateTime;
    }
}
