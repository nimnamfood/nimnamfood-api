package nimnamfood.query.recipe;

import nimnamfood.infrastructure.repository.jdbc.WithJdbcRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithJdbcRepositories.class})
public class FindRecipesHandlerTest extends PostgresTestContainerBase {
    @Autowired
    NamedParameterJdbcTemplate template;

    RecipeService recipeService = Mockito.mock();

    @Test
    void returnsAnEmptyListOfRecipes() {
        FindRecipesHandler handler = new FindRecipesHandler(recipeService);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes(), template);

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllRecipesWhenNoQueryIsProvided() {
        FindRecipesHandler handler = new FindRecipesHandler(recipeService);
        Recipe recipe1 = Recipe.factory().create("recette", UUID.randomUUID(), 1, Collections.emptySet(), "", Collections.emptySet())._1;
        Mockito.when(recipeService.illustrationUrl(recipe1.getIllustrationId())).thenReturn("recipe1 illu url");
        Tag tag = Tag.factory().create("tag")._1;
        Repositories.tags().add(tag);
        Recipe recipe2 = Recipe.factory().create("recette 2", null, 1, Collections.emptySet(), "", Set.of(tag.getId()))._1;
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes(), template);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id().equals(recipe1.getId()) &&
                summary.name().equals(recipe1.getName()) && summary.illustrationUrl().equals("recipe1 illu url") &&
                summary.tags().isEmpty());
        assertThat(result).anyMatch(summary -> summary.id().equals(recipe2.getId()) &&
                summary.name().equals(recipe2.getName()) &&
                summary.tags().stream().findFirst().get().name().equals("tag"));
    }

    @Test
    void returnsAllRecipesContainingTheQuery() {
        FindRecipesHandler handler = new FindRecipesHandler(recipeService);

        Recipe recipe1 = Recipe.factory().create("poulet citron", null, 1, Collections.emptySet(), "", Collections.emptySet())._1;
        Recipe recipe2 = Recipe.factory().create("crevettes", null, 1, Collections.emptySet(), "", Collections.emptySet())._1;
        Recipe recipe3 = Recipe.factory().create("riz au poulet", null, 1, Collections.emptySet(), "", Collections.emptySet())._1;
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("poulet"), template);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id().equals(recipe1.getId()) &&
                summary.name().equals(recipe1.getName()) &&
                summary.tags().isEmpty());
        assertThat(result).anyMatch(summary -> summary.id().equals(recipe3.getId()) &&
                summary.name().equals(recipe3.getName()) &&
                summary.tags().isEmpty());
    }

    @Test
    void ignoresTheQueryCaseAndSpecialCharacters() {
        FindRecipesHandler handler = new FindRecipesHandler(recipeService);
        Repositories.recipes().add(Recipe.factory().create(
                "taboulé de poulet", null, 1, Collections.emptySet(), "", Collections.emptySet())._1);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("taboule"), template);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("taboulé de poulet");
    }

    @Test
    void returnsAllRecipesHavingAtLeastAllRequestedTags() {
        FindRecipesHandler handler = new FindRecipesHandler(recipeService);

        Tag tag1 = Tag.factory().create("1")._1;
        Tag tag2 = Tag.factory().create("2")._1;
        Tag tag3 = Tag.factory().create("3")._1;
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);
        Repositories.tags().add(tag3);

        Recipe recipe1 = Recipe.factory().create("1", null, 1, Collections.emptySet(), "", Set.of(tag2.getId(), tag3.getId()))._1;
        Recipe recipe2 = Recipe.factory().create("2", null, 1, Collections.emptySet(), "", Set.of(tag2.getId(), tag1.getId()))._1;
        Recipe recipe3 = Recipe.factory().create("3", null, 1, Collections.emptySet(), "", Set.of())._1;
        Recipe recipe4 = Recipe.factory().create("4", null, 1, Collections.emptySet(), "", Set.of(tag1.getId(), tag2.getId(), tag3.getId()))._1;
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);
        Repositories.recipes().add(recipe4);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes(Set.of(tag3.getId().toString(), tag2.getId().toString())), template);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id().equals(recipe1.getId()));

        final RecipeSearchSummary summaryOfRecipe4 = result.stream().filter(s -> s.id().equals(recipe4.getId())).findFirst().get();
        assertThat(summaryOfRecipe4.name()).isEqualTo("4");
        assertThat(summaryOfRecipe4.tags()).hasSize(3);
        assertThat(summaryOfRecipe4.tags()).anyMatch(s -> s.id().equals(tag3.getId()) && s.name().equals("3"));
        assertThat(summaryOfRecipe4.tags()).anyMatch(s -> s.id().equals(tag2.getId()) && s.name().equals("2"));
        assertThat(summaryOfRecipe4.tags()).anyMatch(s -> s.id().equals(tag1.getId()) && s.name().equals("1"));

    }

    @Test
    void returnsAllRecipesContainingTheQueryAndHavingAtLeastAllRequestedTags() {
        FindRecipesHandler handler = new FindRecipesHandler(recipeService);

        Tag tag1 = Tag.factory().create("1")._1;
        Tag tag2 = Tag.factory().create("2")._1;
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);

        Recipe recipe1 = Recipe.factory().create("poulet au citron", null, 1, Collections.emptySet(), "", Set.of(tag1.getId()))._1;
        Recipe recipe2 = Recipe.factory().create("pâtes au poulet", null, 1, Collections.emptySet(), "", Set.of(tag2.getId(), tag1.getId()))._1;
        Recipe recipe3 = Recipe.factory().create("pâtes au poulet et citron", null, 1, Collections.emptySet(), "", Set.of(tag2.getId()))._1;
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("poulet", Set.of(tag2.getId().toString(), tag1.getId().toString())), template);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(recipe2.getId());
    }

    @Test
    void paginatesTheRecipesInReversedCreationOrder() {
        FindRecipesHandler handler = new FindRecipesHandler(recipeService);

        Tag tag1 = Tag.factory().create("1")._1;
        Repositories.tags().add(tag1);

        Recipe recipe1 = Recipe.factory().create("recette 1", 1, Collections.emptySet(), "", Set.of(tag1.getId()))._1;
        Recipe recipe2 = Recipe.factory().create("recette 2", 1, Collections.emptySet(), "", Set.of(tag1.getId()))._1;
        Recipe recipe3 = Recipe.factory().create("recette 3", 1, Collections.emptySet(), "", Set.of(tag1.getId()))._1;
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);
        Repositories.recipes().add(recipe1);

        List<RecipeSearchSummary> result1 = handler.execute((FindRecipes) new FindRecipes().limit(1).skip(0), template);
        List<RecipeSearchSummary> result2 = handler.execute((FindRecipes) new FindRecipes("recette").limit(1).skip(1), template);
        List<RecipeSearchSummary> result3 = handler.execute((FindRecipes) new FindRecipes(Set.of(tag1.getId().toString())).limit(1).skip(2), template);
        List<RecipeSearchSummary> result4 = handler.execute((FindRecipes) new FindRecipes("recette", Set.of(tag1.getId().toString())).limit(2).skip(0), template);
        List<RecipeSearchSummary> result5 = handler.execute(new FindRecipes(), template);

        assertThat(result1).first().extracting(RecipeSearchSummary::id).isEqualTo(recipe3.getId());
        assertThat(result2).first().extracting(RecipeSearchSummary::id).isEqualTo(recipe2.getId());
        assertThat(result3).first().extracting(RecipeSearchSummary::id).isEqualTo(recipe1.getId());
        assertThat(result4).matches(r -> r.get(0).id().equals(recipe3.getId()) && r.get(1).id().equals(recipe2.getId()));
        assertThat(result5).matches(r -> r.get(0).id().equals(recipe3.getId()) && r.get(1).id().equals(recipe2.getId()) && r.get(2).id().equals(recipe1.getId()));
    }
}
