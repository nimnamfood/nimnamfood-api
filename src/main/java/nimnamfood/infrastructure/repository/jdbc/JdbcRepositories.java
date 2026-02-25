package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcRepository;
import nimnamfood.infrastructure.repository.jdbc.plan.PlanJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.plan.PlanJdbcRepository;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeJdbcRepository;
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcRepository;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.IngredientRepository;
import nimnamfood.model.plan.PlanRepository;
import nimnamfood.model.recipe.RecipeRepository;
import nimnamfood.model.tag.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;

public class JdbcRepositories extends Repositories {
    private JdbcAggregateTemplate jdbcAggregateTemplate;
    private TagJdbcCrudRepository tagJdbcCrudRepository;
    private IngredientJdbcCrudRepository ingredientJdbcCrudRepository;
    private RecipeJdbcCrudRepository recipeJdbcCrudRepository;
    private PlanJdbcCrudRepository planJdbcCrudRepository;

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

    @Override
    protected PlanRepository getPlans() {
        return new PlanJdbcRepository(this.planJdbcCrudRepository, this.jdbcAggregateTemplate);
    }

    @Autowired
    public void setJdbcAggregateTemplate(JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Autowired
    public void setTagJdbcCrudRepository(TagJdbcCrudRepository tagJdbcCrudRepository) {
        this.tagJdbcCrudRepository = tagJdbcCrudRepository;
    }

    @Autowired
    public void setIngredientJdbcCrudRepository(IngredientJdbcCrudRepository ingredientJdbcCrudRepository) {
        this.ingredientJdbcCrudRepository = ingredientJdbcCrudRepository;
    }

    @Autowired
    public void setRecipeJdbcCrudRepository(RecipeJdbcCrudRepository recipeJdbcCrudRepository) {
        this.recipeJdbcCrudRepository = recipeJdbcCrudRepository;
    }

    @Autowired
    public void setPlanJdbcCrudRepository(PlanJdbcCrudRepository planJdbcCrudRepository) {
        this.planJdbcCrudRepository = planJdbcCrudRepository;
    }
}
