package nimnamfood.model.recipe;

import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeDbo;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeTagDbo;
import vtertre.ddd.BaseAggregateRootWithUuid;

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
        public Recipe create(String name, UUID illustrationId, int portionsCount,
                             Set<RecipeIngredient> ingredients, String instructions, Set<UUID> tagIds) {
            return new Recipe(name, illustrationId, portionsCount, ingredients, instructions, tagIds);
        }

        public Recipe create(String name, int portionsCount, Set<RecipeIngredient> ingredients, String instructions,
                             Set<UUID> tagIds) {
            return this.create(name, null, portionsCount, ingredients, instructions, tagIds);
        }

        public Recipe create(String name, int portionsCount, Set<RecipeIngredient> ingredients, String instructions) {
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

    public Recipe updated(String name, UUID illustrationId, int portionsCount,
                          Set<RecipeIngredient> ingredients, String instructions, Set<UUID> tagIds) {
        return new Recipe(this.getId(), name, illustrationId, portionsCount, ingredients, instructions, tagIds,
                this.creationDateTime);
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
