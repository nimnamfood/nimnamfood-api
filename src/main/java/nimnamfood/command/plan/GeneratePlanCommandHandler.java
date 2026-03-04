package nimnamfood.command.plan;

import com.google.common.collect.Sets;
import nimnamfood.infrastructure.repository.RecipeTagFilterRequirement;
import nimnamfood.model.Repositories;
import nimnamfood.model.plan.Meal;
import nimnamfood.model.plan.Plan;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class GeneratePlanCommandHandler implements CommandHandler<GeneratePlanCommand, UUID> {
    @Override
    public Tuple<UUID, List<DomainEvent>> execute(GeneratePlanCommand command) {
        final var globalRequired = toUuids(command.globalTagFilters.has);
        final var globalExcluded = toUuids(command.globalTagFilters.doesNotHave);
        final var globalOneOf = command.globalTagFilters.hasOneOf.stream()
                .map(GeneratePlanCommandHandler::toUuids)
                .toList();

        final var meals = command.meals.stream().map(mealCommand -> {
            final var requirement = combineFilters(globalRequired, globalExcluded, globalOneOf, mealCommand.tagFilters);
            final var candidateIds = new ArrayList<>(Repositories.recipes().findIdsByTagFilterRequirement(requirement));

            if (candidateIds.isEmpty()) {
                return new Meal(mealCommand.mealIndex, null);
            }

            final var selectedRecipeId = candidateIds.get(ThreadLocalRandom.current().nextInt(candidateIds.size()));
            return new Meal(mealCommand.mealIndex, selectedRecipeId);
        }).collect(Collectors.toSet());

        final var tuple = Plan.factory().create(meals);
        Repositories.plans().add(tuple._1);

        return tuple.map((plan, event) -> Tuple.of(plan.getId(), List.of(event)));
    }

    private static RecipeTagFilterRequirement combineFilters(Set<UUID> globalRequired, Set<UUID> globalExcluded,
                                                             List<Set<UUID>> globalOneOf, TagFilterCommandPart localFilters) {
        final var required = Sets.union(globalRequired, toUuids(localFilters.has));
        final var excluded = Sets.union(globalExcluded, toUuids(localFilters.doesNotHave));

        final var oneOf = new ArrayList<>(globalOneOf);
        localFilters.hasOneOf.stream()
                .map(GeneratePlanCommandHandler::toUuids)
                .forEach(oneOf::add);

        return new RecipeTagFilterRequirement(required, excluded, oneOf);
    }

    private static Set<UUID> toUuids(Set<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return Collections.emptySet();
        }

        return strings.stream().map(UUID::fromString).collect(Collectors.toUnmodifiableSet());
    }
}
