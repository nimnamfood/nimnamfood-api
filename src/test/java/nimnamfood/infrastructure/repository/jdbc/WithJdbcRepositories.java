package nimnamfood.infrastructure.repository.jdbc;

import nimnamfood.infrastructure.repository.jdbc.ingredient.IngredientJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.plan.PlanJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.recipe.RecipeJdbcCrudRepository;
import nimnamfood.infrastructure.repository.jdbc.tag.TagJdbcCrudRepository;
import nimnamfood.model.Repositories;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class WithJdbcRepositories implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        final JdbcAggregateTemplate template = SpringExtension.getApplicationContext(extensionContext).getBean(JdbcAggregateTemplate.class);
        final TagJdbcCrudRepository tagJdbcCrudRepository = SpringExtension.getApplicationContext(extensionContext).getBean(TagJdbcCrudRepository.class);
        final IngredientJdbcCrudRepository ingredientJdbcCrudRepository = SpringExtension.getApplicationContext(extensionContext).getBean(IngredientJdbcCrudRepository.class);
        final RecipeJdbcCrudRepository recipeJdbcCrudRepository = SpringExtension.getApplicationContext(extensionContext).getBean(RecipeJdbcCrudRepository.class);
        final PlanJdbcCrudRepository planJdbcCrudRepository = SpringExtension.getApplicationContext(extensionContext).getBean(PlanJdbcCrudRepository.class);

        final JdbcRepositories jdbcRepositories = new JdbcRepositories();
        jdbcRepositories.setJdbcAggregateTemplate(template);
        jdbcRepositories.setTagJdbcCrudRepository(tagJdbcCrudRepository);
        jdbcRepositories.setIngredientJdbcCrudRepository(ingredientJdbcCrudRepository);
        jdbcRepositories.setRecipeJdbcCrudRepository(recipeJdbcCrudRepository);
        jdbcRepositories.setPlanJdbcCrudRepository(planJdbcCrudRepository);

        Repositories.initialize(jdbcRepositories);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        Repositories.initialize(null);
    }
}
