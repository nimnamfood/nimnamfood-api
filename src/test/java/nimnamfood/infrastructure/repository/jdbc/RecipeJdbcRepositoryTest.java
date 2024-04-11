package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientDbo;
import nimnamfood.infrastructure.repository.jdbc.recipe.*;
import nimnamfood.infrastructure.repository.jdbc.tag.TagDbo;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RecipeJdbcRepositoryTest extends PostgresTestContainerBase {
    @Autowired
    RecipeJdbcCrudRepository crudRepository;
    @Autowired
    JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void retrievesARecipe() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate);
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
        recipeIngredientDbo.setQuantityFixed(true);

        RecipeDbo recipeDbo = new RecipeDbo();
        recipeDbo.setId(UUID.randomUUID());
        recipeDbo.setName("recette");
        recipeDbo.setIllustrationId(UUID.randomUUID());
        recipeDbo.setPortionsCount(2);
        recipeDbo.setInstructions("instructions");
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
        assertThat(foundRecipe.get().getTagIds()).containsExactly(tagDbo.getId());

        final RecipeIngredient recipeIngredient = foundRecipe.get().getIngredients().stream().findFirst().get();
        assertThat(recipeIngredient.getId()).isEqualTo(recipeIngredientDbo.getId());
        assertThat(recipeIngredient.ingredientId()).isEqualTo(ingredientDbo.getId());
        assertThat(recipeIngredient.quantity()).isEqualTo(10f);
        assertThat(recipeIngredient.unit()).isEqualTo(IngredientUnit.PIECE);
        assertThat(recipeIngredient.quantityFixed()).isTrue();
    }

    @Test
    void addsARecipe() {
        RecipeJdbcRepository repository = new RecipeJdbcRepository(crudRepository, jdbcAggregateTemplate);
        UUID tagId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        RecipeIngredient recipeIngredient = new RecipeIngredient(ingredientId, 3, IngredientUnit.PINCH, false);
        Recipe recipe = Recipe.factory().create("autre recette", UUID.randomUUID(), 1, Set.of(recipeIngredient), "autres", Set.of(tagId));

        repository.add(recipe);
        RecipeDbo dbo = this.jdbcAggregateTemplate.findById(recipe.getId(), RecipeDbo.class);

        assertThat(dbo).isNotNull();
        assertThat(dbo.getName()).isEqualTo("autre recette");
        assertThat(dbo.getIllustrationId()).isEqualTo(recipe.getIllustrationId());
        assertThat(dbo.getPortionsCount()).isEqualTo(1);
        assertThat(dbo.getInstructions()).isEqualTo("autres");
        assertThat(dbo.getTags().stream().findFirst().get().getTagId()).isEqualTo(tagId);

        final RecipeIngredientDbo recipeIngredientDbo = dbo.getIngredients().stream().findFirst().get();
        assertThat(recipeIngredientDbo.getId()).isEqualTo(recipeIngredient.getId());
        assertThat(recipeIngredientDbo.getIngredientId()).isEqualTo(ingredientId);
        assertThat(recipeIngredientDbo.getQuantity()).isEqualTo(3);
        assertThat(recipeIngredientDbo.getUnit()).isEqualTo(IngredientUnit.PINCH);
        assertThat(recipeIngredientDbo.getQuantityFixed()).isFalse();
    }
}
