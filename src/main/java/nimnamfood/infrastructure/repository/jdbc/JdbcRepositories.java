package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcRepository;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeJdbcRepository;
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcRepository;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.IngredientRepository;
import nimnamfood.model.recipe.RecipeRepository;
import nimnamfood.model.tag.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;

public class JdbcRepositories extends Repositories {
    @Autowired
    private JdbcAggregateTemplate jdbcAggregateTemplate;
    @Autowired
    private TagJdbcCrudRepository tagJdbcCrudRepository;
    @Autowired
    private IngredientJdbcCrudRepository ingredientJdbcCrudRepository;
    @Autowired
    private RecipeJdbcCrudRepository recipeJdbcCrudRepository;

    @Override
    protected TagRepository getTags() {
        return new TagJdbcRepository(this.tagJdbcCrudRepository, this.jdbcAggregateTemplate);
    }

    @Override
    protected IngredientRepository getIngredients() {
        return new IngredientJdbcRepository(this.ingredientJdbcCrudRepository, this.jdbcAggregateTemplate);
    }

    @Override
    protected RecipeRepository getRecipes() {
        return new RecipeJdbcRepository(this.recipeJdbcCrudRepository, this.jdbcAggregateTemplate);
    }
}
