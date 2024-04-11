package nimnamfood.infrastructure.repository.jdbc.recipe;

import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.recipe.RecipeRepository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import vtertre.infrastructure.persistence.jdbc.JdbcRepositoryWithUuid;

import java.util.stream.Collectors;

public class RecipeJdbcRepository extends JdbcRepositoryWithUuid<Recipe, RecipeDbo> implements RecipeRepository {

    public RecipeJdbcRepository(RecipeJdbcCrudRepository jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        super(jdbcCrudRepository, jdbcAggregateTemplate);
    }

    @Override
    public RecipeDbo toDbo(Recipe recipe) {
        final RecipeDbo dbo = new RecipeDbo();
        dbo.setId(recipe.getId());
        dbo.name = recipe.getName();
        dbo.illustrationId = recipe.getIllustrationId();
        dbo.portionsCount = recipe.getPortionsCount();
        dbo.instructions = recipe.getInstructions();
        dbo.ingredients = recipe.getIngredients().stream()
                .map(RecipeJdbcRepository::recipeIngredientDbo).collect(Collectors.toSet());
        dbo.tags = recipe.getTagIds().stream().map(tagId -> {
            final RecipeTagDbo recipeTagDbo = new RecipeTagDbo();
            recipeTagDbo.tagId = tagId;
            return recipeTagDbo;
        }).collect(Collectors.toSet());
        return dbo;
    }

    private static RecipeIngredientDbo recipeIngredientDbo(RecipeIngredient recipeIngredient) {
        final RecipeIngredientDbo recipeIngredientDbo = new RecipeIngredientDbo();
        recipeIngredientDbo.id = recipeIngredient.getId();
        recipeIngredientDbo.ingredientId = recipeIngredient.ingredientId();
        recipeIngredientDbo.quantity = recipeIngredient.quantity();
        recipeIngredientDbo.unit = recipeIngredient.unit();
        recipeIngredientDbo.quantityFixed = recipeIngredient.quantityFixed();
        return recipeIngredientDbo;
    }
}
