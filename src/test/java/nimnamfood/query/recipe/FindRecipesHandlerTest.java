package nimnamfood.query.recipe;

import nimnamfood.infrastructure.repository.jdbc.WithJdbcRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.PostgresTestContainerBase;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithJdbcRepositories.class})
public class FindRecipesHandlerTest extends PostgresTestContainerBase {
    @Autowired
    NamedParameterJdbcTemplate template;

    @Test
    void returnsAnEmptyListOfRecipes() {
        FindRecipesHandler handler = new FindRecipesHandler();

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes(), template);

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsAllRecipesWhenNoQueryIsProvided() {
        FindRecipesHandler handler = new FindRecipesHandler();
        Recipe recipe1 = Recipe.factory().create("recette", 1, Collections.emptySet(), "", Collections.emptySet());
        Tag tag = new Tag("tag");
        Repositories.tags().add(tag);
        Recipe recipe2 = Recipe.factory().create("recette 2", 1, Collections.emptySet(), "", Set.of(tag.getId()));
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes(), template);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe1.getId()) &&
                summary.name.equals(recipe1.getName()) &&
                summary.tags.isEmpty());
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe2.getId()) &&
                summary.name.equals(recipe2.getName()) &&
                summary.tags.stream().findFirst().get().name.equals("tag"));
    }

    @Test
    void returnsAllRecipesContainingTheQuery() {
        FindRecipesHandler handler = new FindRecipesHandler();

        Recipe recipe1 = Recipe.factory().create("poulet citron", 1, Collections.emptySet(), "", Collections.emptySet());
        Recipe recipe2 = Recipe.factory().create("crevettes", 1, Collections.emptySet(), "", Collections.emptySet());
        Recipe recipe3 = Recipe.factory().create("riz au poulet", 1, Collections.emptySet(), "", Collections.emptySet());
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("poulet"), template);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe1.getId()) &&
                summary.name.equals(recipe1.getName()) &&
                summary.tags.isEmpty());
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe3.getId()) &&
                summary.name.equals(recipe3.getName()) &&
                summary.tags.isEmpty());
    }

    @Disabled("Désactivé le temps de trouver comment ignorer les caractères spéciaux côté DB ou via les projections")
    @Test
    void ignoresTheQueryCaseAndSpecialCharacters() {
        FindRecipesHandler handler = new FindRecipesHandler();
        Repositories.recipes().add(Recipe.factory().create(
                "taboulé de poulet", 1, Collections.emptySet(), "", Collections.emptySet()));

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("taboule"), template);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name).isEqualTo("taboulé de poulet");
    }

    @Test
    void returnsAllRecipesHavingAtLeastAllRequestedTags() {
        FindRecipesHandler handler = new FindRecipesHandler();

        Tag tag1 = new Tag("1");
        Tag tag2 = new Tag("2");
        Tag tag3 = new Tag("3");
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);
        Repositories.tags().add(tag3);

        Recipe recipe1 = Recipe.factory().create("1", 1, Collections.emptySet(), "", Set.of(tag2.getId(), tag3.getId()));
        Recipe recipe2 = Recipe.factory().create("2", 1, Collections.emptySet(), "", Set.of(tag2.getId(), tag1.getId()));
        Recipe recipe3 = Recipe.factory().create("3", 1, Collections.emptySet(), "", Set.of());
        Recipe recipe4 = Recipe.factory().create("4", 1, Collections.emptySet(), "", Set.of(tag1.getId(), tag2.getId(), tag3.getId()));
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);
        Repositories.recipes().add(recipe4);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes(Set.of(tag3.getId().toString(), tag2.getId().toString())), template);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(summary -> summary.id.equals(recipe1.getId()));

        final RecipeSearchSummary summaryOfRecipe4 = result.stream().filter(s -> s.id.equals(recipe4.getId())).findFirst().get();
        assertThat(summaryOfRecipe4.name).isEqualTo("4");
        assertThat(summaryOfRecipe4.tags).hasSize(3);
        assertThat(summaryOfRecipe4.tags).anyMatch(s -> s.id.equals(tag3.getId()) && s.name.equals("3"));
        assertThat(summaryOfRecipe4.tags).anyMatch(s -> s.id.equals(tag2.getId()) && s.name.equals("2"));
        assertThat(summaryOfRecipe4.tags).anyMatch(s -> s.id.equals(tag1.getId()) && s.name.equals("1"));

    }

    @Test
    void returnsAllRecipesContainingTheQueryAndHavingAtLeastAllRequestedTags() {
        FindRecipesHandler handler = new FindRecipesHandler();

        Tag tag1 = new Tag("1");
        Tag tag2 = new Tag("2");
        Repositories.tags().add(tag1);
        Repositories.tags().add(tag2);

        Recipe recipe1 = Recipe.factory().create("poulet au citron", 1, Collections.emptySet(), "", Set.of(tag1.getId()));
        Recipe recipe2 = Recipe.factory().create("pâtes au poulet", 1, Collections.emptySet(), "", Set.of(tag2.getId(), tag1.getId()));
        Recipe recipe3 = Recipe.factory().create("pâtes au poulet et citron", 1, Collections.emptySet(), "", Set.of(tag2.getId()));
        Repositories.recipes().add(recipe1);
        Repositories.recipes().add(recipe2);
        Repositories.recipes().add(recipe3);

        List<RecipeSearchSummary> result = handler.execute(new FindRecipes("poulet", Set.of(tag2.getId().toString(), tag1.getId().toString())), template);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id).isEqualTo(recipe2.getId());
    }
}
