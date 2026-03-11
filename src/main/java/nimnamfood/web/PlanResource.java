package nimnamfood.web;

import nimnamfood.query.plan.GetPlan;
import nimnamfood.query.plan.model.PlanSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vtertre.query.QueryBus;

import java.util.concurrent.Future;

@RestController
public class PlanResource {
    private final QueryBus queryBus;

    @Autowired
    public PlanResource(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/plans/{stringUuid}")
    public Future<PlanSummary> get(@PathVariable String stringUuid) {
        return this.queryBus.dispatch(new GetPlan(stringUuid));
    }
}
