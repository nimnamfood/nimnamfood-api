package nimnamfood.command.plan;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.Plan;
import nimnamfood.model.plan.PlanCreated;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.tag.Tag;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.BusinessError;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(WithMemoryRepositories.class)
public class GeneratePlanCommandHandlerTest {

    @Test
    void generatesAPlanWithRandomRecipesMatchingGlobalTagFilters() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Tag tag = Tag.factory().create("rapide")._1;
        Repositories.tags().add(tag);

        Recipe recipe = Recipe.factory().create("poulet", 2, Collections.emptySet(), "", Set.of(tag.getId()))._1;
        Repositories.recipes().add(recipe);

        Recipe recipeWithoutTag = Recipe.factory().create("salade", 1, Collections.emptySet(), "")._1;
        Repositories.recipes().add(recipeWithoutTag);

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.globalTagFilters.has = Set.of(tag.getId().toString());
        command.meals = List.of(mealConfig(0));

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        Plan plan = Repositories.plans().get(result._1).get();
        assertThat(plan.meals()).hasSize(1);
        assertThat(plan.meals()).first().extracting(Meal::recipeId).isEqualTo(recipe.getId());
    }

    @Test
    void generatesAPlanWithLocalTagFiltersPerMeal() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Tag tagA = Tag.factory().create("A")._1;
        Tag tagB = Tag.factory().create("B")._1;
        Repositories.tags().add(tagA);
        Repositories.tags().add(tagB);

        Recipe recipeA = Recipe.factory().create("recipeA", 1, Collections.emptySet(), "", Set.of(tagA.getId()))._1;
        Recipe recipeB = Recipe.factory().create("recipeB", 1, Collections.emptySet(), "", Set.of(tagB.getId()))._1;
        Repositories.recipes().add(recipeA);
        Repositories.recipes().add(recipeB);

        MealConfigurationCommandPart meal0 = mealConfig(0);
        meal0.tagFilters.has = Set.of(tagA.getId().toString());

        MealConfigurationCommandPart meal1 = mealConfig(1);
        meal1.tagFilters.has = Set.of(tagB.getId().toString());

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.meals = List.of(meal0, meal1);

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        Plan plan = Repositories.plans().get(result._1).get();
        assertThat(plan.meals()).hasSize(2);

        Meal mealAtIndex0 = plan.meals().stream().filter(m -> m.mealIndex() == 0).findFirst().get();
        Meal mealAtIndex1 = plan.meals().stream().filter(m -> m.mealIndex() == 1).findFirst().get();
        assertThat(mealAtIndex0.recipeId()).isEqualTo(recipeA.getId());
        assertThat(mealAtIndex1.recipeId()).isEqualTo(recipeB.getId());
    }

    @Test
    void combinesGlobalAndLocalFilters() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Tag globalTag = Tag.factory().create("global")._1;
        Tag localTag = Tag.factory().create("local")._1;
        Repositories.tags().add(globalTag);
        Repositories.tags().add(localTag);

        Recipe matchingRecipe = Recipe.factory().create("match", 1, Collections.emptySet(), "",
                Set.of(globalTag.getId(), localTag.getId()))._1;
        Recipe onlyGlobalRecipe = Recipe.factory().create("global-only", 1, Collections.emptySet(), "",
                Set.of(globalTag.getId()))._1;
        Repositories.recipes().add(matchingRecipe);
        Repositories.recipes().add(onlyGlobalRecipe);

        MealConfigurationCommandPart meal0 = mealConfig(0);
        meal0.tagFilters.has = Set.of(localTag.getId().toString());

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.globalTagFilters.has = Set.of(globalTag.getId().toString());
        command.meals = List.of(meal0);

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        Plan plan = Repositories.plans().get(result._1).get();
        assertThat(plan.meals()).first().extracting(Meal::recipeId).isEqualTo(matchingRecipe.getId());
    }

    @Test
    void supportsDoesNotHaveFilter() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Tag excludedTag = Tag.factory().create("excluded")._1;
        Repositories.tags().add(excludedTag);

        Recipe recipeWithTag = Recipe.factory().create("with-tag", 1, Collections.emptySet(), "",
                Set.of(excludedTag.getId()))._1;
        Recipe recipeWithoutTag = Recipe.factory().create("without-tag", 1, Collections.emptySet(), "")._1;
        Repositories.recipes().add(recipeWithTag);
        Repositories.recipes().add(recipeWithoutTag);

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.globalTagFilters.doesNotHave = Set.of(excludedTag.getId().toString());
        command.meals = List.of(mealConfig(0));

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        Plan plan = Repositories.plans().get(result._1).get();
        assertThat(plan.meals()).first().extracting(Meal::recipeId).isEqualTo(recipeWithoutTag.getId());
    }

    @Test
    void supportsHasOneOfFilter() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Tag tagA = Tag.factory().create("A")._1;
        Tag tagB = Tag.factory().create("B")._1;
        Tag tagC = Tag.factory().create("C")._1;
        Repositories.tags().add(tagA);
        Repositories.tags().add(tagB);
        Repositories.tags().add(tagC);

        Recipe recipeA = Recipe.factory().create("A", 1, Collections.emptySet(), "", Set.of(tagA.getId(), tagC.getId()))._1;
        Recipe recipeB = Recipe.factory().create("B", 1, Collections.emptySet(), "", Set.of(tagC.getId()))._1;
        Repositories.recipes().add(recipeA);
        Repositories.recipes().add(recipeB);

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.globalTagFilters.hasOneOf = List.of(Set.of(tagA.getId().toString(), tagB.getId().toString()));
        command.meals = List.of(mealConfig(0));

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        Plan plan = Repositories.plans().get(result._1).get();
        assertThat(plan.meals()).first().extracting(Meal::recipeId).isEqualTo(recipeA.getId());
    }

    @Test
    void combineAllFilters() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Tag tagA = Tag.factory().create("A")._1;
        Tag tagB = Tag.factory().create("B")._1;
        Tag tagC = Tag.factory().create("C")._1;
        Tag tagD = Tag.factory().create("D")._1;
        Repositories.tags().add(tagA);
        Repositories.tags().add(tagB);
        Repositories.tags().add(tagC);
        Repositories.tags().add(tagD);

        Recipe recipeA = Recipe.factory().create("A", 1, Collections.emptySet(), "", Set.of(tagD.getId(), tagC.getId()))._1;
        Recipe recipeB = Recipe.factory().create("B", 1, Collections.emptySet(), "", Set.of(tagA.getId(), tagB.getId(), tagC.getId()))._1;
        Recipe recipeC = Recipe.factory().create("C", 1, Collections.emptySet(), "", Set.of(tagC.getId(), tagA.getId()))._1;
        Repositories.recipes().add(recipeA);
        Repositories.recipes().add(recipeB);
        Repositories.recipes().add(recipeC);

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.globalTagFilters.has = Set.of(tagA.getId().toString());
        command.globalTagFilters.doesNotHave = Set.of(tagB.getId().toString());
        command.globalTagFilters.hasOneOf = List.of(Set.of(tagC.getId().toString(), tagD.getId().toString()));
        command.meals = List.of(mealConfig(0));

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        Plan plan = Repositories.plans().get(result._1).get();
        assertThat(plan.meals()).first().extracting(Meal::recipeId).isEqualTo(recipeC.getId());
    }

    @Test
    void emitsAPlanCreatedDomainEvent() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Recipe recipe = Recipe.factory().create("recette", 1, Collections.emptySet(), "")._1;
        Repositories.recipes().add(recipe);

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.meals = List.of(mealConfig(0));

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        assertThat(result._2).hasSize(1).first()
                .asInstanceOf(InstanceOfAssertFactories.type(PlanCreated.class))
                .satisfies(event -> {
                    assertThat(event.id()).isEqualTo(result._1);
                    assertThat(event.meals()).hasSize(1);
                    assertThat(event.meals()).first().extracting(Meal::recipeId).isEqualTo(recipe.getId());
                });
    }

    @Test
    void setsRecipeIdToNullWhenNoRecipeMatchesFilters() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Tag tag = Tag.factory().create("tag")._1;
        Repositories.tags().add(tag);

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.globalTagFilters.has = Set.of(tag.getId().toString());
        command.meals = List.of(mealConfig(0));

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        Plan plan = Repositories.plans().get(result._1).get();
        assertThat(plan.meals()).hasSize(1);
        assertThat(plan.meals().iterator().next().recipeId()).isNull();
    }

    @Test
    void throwsWhenTwoMealsHaveTheSameIndex() {
        GeneratePlanCommandHandler handler = new GeneratePlanCommandHandler();

        Recipe recipe = Recipe.factory().create("recette", 1, Collections.emptySet(), "")._1;
        Repositories.recipes().add(recipe);

        GeneratePlanCommand command = new GeneratePlanCommand();
        command.meals = List.of(mealConfig(0), mealConfig(0));

        assertThatExceptionOfType(BusinessError.class)
                .isThrownBy(() -> handler.execute(command))
                .withMessage("DUPLICATE_MEAL_INDEX");
    }

    private static MealConfigurationCommandPart mealConfig(int index) {
        MealConfigurationCommandPart config = new MealConfigurationCommandPart();
        config.mealIndex = index;
        config.tagFilters = new TagFilterCommandPart();
        return config;
    }
}
