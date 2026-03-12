package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.RecipeTagFilterRequirement;
import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientDbo;
import nimnamfood.infrastructure.repository.jdbc.recipe.*;
import nimnamfood.infrastructure.repository.jdbc.tag.TagDbo;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class RecipeJdbcRepositoryTest extends PostgresTestContainerBase {
    @Autowired
    RecipeJdbcCrudRepository crudRepository;
    @Autowired
    JdbcAggregateTemplate jdbcAggregateTemplate;
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    void retrievesARecipe() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);
        TagDbo tagDbo = new TagDbo();
        tagDbo.setId(UUID.randomUUID());
        tagDbo.setName("rapide");

        IngredientDbo ingredientDbo = new IngredientDbo();
        ingredientDbo.setId(UUID.randomUUID());
        ingredientDbo.setName("citron");
        ingredientDbo.setUnit(IngredientUnit.GRAM);

        RecipeTagDbo recipeTagDbo = new RecipeTagDbo();
        recipeTagDbo.setTagId(tagDbo.getId());

        RecipeIngredientDbo recipeIngredientDbo = new RecipeIngredientDbo();
        recipeIngredientDbo.setId(UUID.randomUUID());
        recipeIngredientDbo.setIngredientId(ingredientDbo.getId());
        recipeIngredientDbo.setQuantity(10f);
        recipeIngredientDbo.setUnit(IngredientUnit.PIECE);

        RecipeDbo recipeDbo = new RecipeDbo();
        recipeDbo.setId(UUID.randomUUID());
        recipeDbo.setName("recette");
        recipeDbo.setIllustrationId(UUID.randomUUID());
        recipeDbo.setPortionsCount(2);
        recipeDbo.setInstructions("instructions");
        recipeDbo.setCreationDateTime(Instant.now());
        recipeDbo.setTags(Set.of(recipeTagDbo));
        recipeDbo.setIngredients(Set.of(recipeIngredientDbo));

        this.jdbcAggregateTemplate.insert(tagDbo);
        this.jdbcAggregateTemplate.insert(ingredientDbo);
        this.jdbcAggregateTemplate.insert(recipeDbo);

        Optional<Recipe> foundRecipe = repository.get(recipeDbo.getId());

        assertThat(foundRecipe).isPresent();
        assertThat(foundRecipe.get().getId()).isEqualTo(recipeDbo.getId());
        assertThat(foundRecipe.get().getName()).isEqualTo("recette");
        assertThat(foundRecipe.get().getIllustrationId()).isEqualTo(recipeDbo.getIllustrationId());
        assertThat(foundRecipe.get().getPortionsCount()).isEqualTo(2);
        assertThat(foundRecipe.get().getInstructions()).isEqualTo("instructions");
        assertThat(foundRecipe.get().getCreationDateTime()).isCloseTo(recipeDbo.getCreationDateTime(), within(1, ChronoUnit.MICROS));
        assertThat(foundRecipe.get().getTagIds()).containsExactly(tagDbo.getId());

        final RecipeIngredient recipeIngredient = foundRecipe.get().getIngredients().stream().findFirst().get();
        assertThat(recipeIngredient.getId()).isEqualTo(recipeIngredientDbo.getId());
        assertThat(recipeIngredient.ingredientId()).isEqualTo(ingredientDbo.getId());
        assertThat(recipeIngredient.quantity()).isEqualTo(10f);
        assertThat(recipeIngredient.unit()).isEqualTo(IngredientUnit.PIECE);
    }

    @Test
    void addsARecipe() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);
        UUID tagId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        RecipeIngredient recipeIngredient = new RecipeIngredient(ingredientId, 3, IngredientUnit.PINCH);
        Recipe recipe = Recipe.factory().create("autre recette", UUID.randomUUID(), 1, Set.of(recipeIngredient), "autres", Set.of(tagId))._1;

        repository.add(recipe);
        RecipeDbo dbo = this.jdbcAggregateTemplate.findById(recipe.getId(), RecipeDbo.class);

        assertThat(dbo).isNotNull();
        assertThat(dbo.getName()).isEqualTo("autre recette");
        assertThat(dbo.getIllustrationId()).isEqualTo(recipe.getIllustrationId());
        assertThat(dbo.getPortionsCount()).isEqualTo(1);
        assertThat(dbo.getInstructions()).isEqualTo("autres");
        assertThat(dbo.getCreationDateTime()).isCloseTo(recipe.getCreationDateTime(), within(1, ChronoUnit.MICROS));
        assertThat(dbo.getTags().stream().findFirst().get().getTagId()).isEqualTo(tagId);

        final RecipeIngredientDbo recipeIngredientDbo = dbo.getIngredients().stream().findFirst().get();
        assertThat(recipeIngredientDbo.getId()).isEqualTo(recipeIngredient.getId());
        assertThat(recipeIngredientDbo.getIngredientId()).isEqualTo(ingredientId);
        assertThat(recipeIngredientDbo.getQuantity()).isEqualTo(3);
        assertThat(recipeIngredientDbo.getUnit()).isEqualTo(IngredientUnit.PINCH);
    }

    @Test
    void findsAllRecipesOnEmptyRequirement() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);

        RecipeDbo recipe1 = insertRecipe("recipe1", Set.of());
        RecipeDbo recipe2 = insertRecipe("recipe2", Set.of());

        Set<UUID> result = repository.findIdsByTagFilterRequirement(
                new RecipeTagFilterRequirement(Set.of(), Set.of(), List.of()));

        assertThat(result).hasSize(2);
        assertThat(result).contains(recipe1.getId(), recipe2.getId());
    }

    @Test
    void findsRecipesByRequiredTags() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);

        TagDbo tagA = insertTag("tagA");
        TagDbo tagB = insertTag("tagB");

        RecipeDbo recipeWithBothTags = insertRecipe("both", Set.of(tagA, tagB));
        insertRecipe("onlyA", Set.of(tagA));
        insertRecipe("none", Set.of());

        Set<UUID> result = repository.findIdsByTagFilterRequirement(
                new RecipeTagFilterRequirement(Set.of(tagA.getId(), tagB.getId()), Set.of(), List.of()));

        assertThat(result).containsExactly(recipeWithBothTags.getId());
    }

    @Test
    void findsRecipesByExcludedTags() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);

        TagDbo excludedTag = insertTag("excluded");

        RecipeDbo recipeWithTag = insertRecipe("withTag", Set.of(excludedTag));
        RecipeDbo recipeWithoutTag = insertRecipe("withoutTag", Set.of());

        Set<UUID> result = repository.findIdsByTagFilterRequirement(
                new RecipeTagFilterRequirement(Set.of(), Set.of(excludedTag.getId()), List.of()));

        assertThat(result).contains(recipeWithoutTag.getId());
        assertThat(result).doesNotContain(recipeWithTag.getId());
    }

    @Test
    void findsRecipesByOneOfTags() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);

        TagDbo tagA = insertTag("oneOfA");
        TagDbo tagB = insertTag("oneOfB");
        TagDbo tagC = insertTag("oneOfC");

        RecipeDbo recipeWithA = insertRecipe("withA", Set.of(tagA));
        insertRecipe("withC", Set.of(tagC));
        insertRecipe("withNone", Set.of());

        Set<UUID> result = repository.findIdsByTagFilterRequirement(
                new RecipeTagFilterRequirement(Set.of(), Set.of(), List.of(Set.of(tagA.getId(), tagB.getId()))));

        assertThat(result).containsExactly(recipeWithA.getId());
    }

    @Test
    void handlesMultipleOneOfCombinations() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);

        TagDbo tagA = insertTag("groupA1");
        TagDbo tagB = insertTag("groupA2");
        TagDbo tagC = insertTag("groupB1");
        TagDbo tagD = insertTag("groupB2");

        RecipeDbo matchesBothGroups = insertRecipe("matchesBoth", Set.of(tagA, tagC));
        insertRecipe("matchesFirst", Set.of(tagA));
        insertRecipe("matchesSecond", Set.of(tagC));

        Set<UUID> result = repository.findIdsByTagFilterRequirement(
                new RecipeTagFilterRequirement(Set.of(), Set.of(),
                        List.of(Set.of(tagA.getId(), tagB.getId()), Set.of(tagC.getId(), tagD.getId()))));

        assertThat(result).containsExactly(matchesBothGroups.getId());
    }

    @Test
    void combinesAllRequirements() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate, namedParameterJdbcTemplate);

        TagDbo requiredTag = insertTag("required");
        TagDbo excludedTag = insertTag("excludedCombined");
        TagDbo oneOfTagA = insertTag("oneOfCombinedA");
        TagDbo oneOfTagB = insertTag("oneOfCombinedB");

        RecipeDbo matching = insertRecipe("matching", Set.of(requiredTag, oneOfTagA));
        insertRecipe("missingRequired", Set.of(oneOfTagA));
        insertRecipe("hasExcluded", Set.of(requiredTag, excludedTag, oneOfTagA));
        insertRecipe("missingOneOf", Set.of(requiredTag));

        Set<UUID> result = repository.findIdsByTagFilterRequirement(
                new RecipeTagFilterRequirement(
                        Set.of(requiredTag.getId()),
                        Set.of(excludedTag.getId()),
                        List.of(Set.of(oneOfTagA.getId(), oneOfTagB.getId()))));

        assertThat(result).containsExactly(matching.getId());
    }

    private TagDbo insertTag(String name) {
        TagDbo tag = new TagDbo();
        tag.setId(UUID.randomUUID());
        tag.setName(name);
        jdbcAggregateTemplate.insert(tag);
        return tag;
    }

    private RecipeDbo insertRecipe(String name, Set<TagDbo> tags) {
        RecipeDbo recipe = new RecipeDbo();
        recipe.setId(UUID.randomUUID());
        recipe.setName(name);
        recipe.setPortionsCount(1);
        recipe.setInstructions("instructions");
        recipe.setCreationDateTime(Instant.now());
        recipe.setTags(tags.stream().map(tag -> {
            RecipeTagDbo recipeTag = new RecipeTagDbo();
            recipeTag.setTagId(tag.getId());
            return recipeTag;
        }).collect(Collectors.toSet()));
        recipe.setIngredients(Set.of());
        jdbcAggregateTemplate.insert(recipe);
        return recipe;
    }
}
