package nimnamfood.query.recipe.projection;

import nimnamfood.model.tag.TagCreated;
import nimnamfood.query.recipe.RecipeSearchViewTestHelper;
import nimnamfood.query.recipe.model.RecipeSearchTagSummaryInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RecipeSearchViewTestHelper.class)
class OnTagCreatedFillRecipeSearchViewPartTest extends PostgresTestContainerBase {
    @Autowired
    RecipeSearchViewTestHelper view;

    @Autowired
    JdbcClient client;

    @Test
    void insertsTheSummaryOfTheTagIntoTheView() {
        TagCreated event = new TagCreated(UUID.randomUUID(), "tag");

        new OnTagCreatedFillRecipeSearchViewPart(client).execute(event);
        RecipeSearchTagSummaryInspector result = view.findTag(event.id()).get();

        assertThat(result.id()).isEqualTo(event.id());
        assertThat(result.name()).isEqualTo("tag");
    }
}