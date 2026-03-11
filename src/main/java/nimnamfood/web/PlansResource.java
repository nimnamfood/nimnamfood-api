package nimnamfood.web;

import nimnamfood.command.plan.GeneratePlanCommand;
import nimnamfood.query.plan.GetPlans;
import nimnamfood.query.plan.model.PlanSearchSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vtertre.command.CommandBus;
import vtertre.query.QueryBus;

import java.util.*;
import java.util.concurrent.Future;

@RestController
public class PlansResource {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    private final int MAX_SEARCH_RESULT = 10;

    @Autowired
    public PlansResource(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @PostMapping("/plans/generate")
    public Future<ResponseEntity<Map<String, UUID>>> generate(@RequestBody GeneratePlanCommand command) {
        return this.commandBus.dispatch(command)
                .thenApply(result -> Collections.singletonMap("id", result))
                .thenApply(result -> new ResponseEntity<>(result, HttpStatus.CREATED));
    }

    @GetMapping("/plans")
    public Future<List<PlanSearchSummary>> get(
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit) {
        final var computedSkip = Optional.ofNullable(skip)
                .map(value -> Math.max(value, 0))
                .orElse(0);

        final var computedLimit = Optional.ofNullable(limit)
                .map(value -> Math.clamp(value, 0, MAX_SEARCH_RESULT))
                .orElse(0);

        return this.queryBus.dispatch(new GetPlans().skip(computedSkip).limit(computedLimit));
    }
}
