package nimnamfood.web;

import nimnamfood.command.plan.GeneratePlanCommand;
import nimnamfood.query.plan.GetPlan;
import nimnamfood.query.plan.model.PlanSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vtertre.command.CommandBus;
import vtertre.query.QueryBus;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@RestController
public class PlansResource {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

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

    @GetMapping("/plans/{stringUuid}")
    public Future<PlanSummary> get(@PathVariable String stringUuid) {
        return this.queryBus.dispatch(new GetPlan(stringUuid));
    }
}
