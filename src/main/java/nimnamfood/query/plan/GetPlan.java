package nimnamfood.query.plan;

import nimnamfood.query.plan.model.PlanSummary;
import vtertre.query.Query;

import java.util.UUID;

public class GetPlan extends Query<PlanSummary> {
    public final UUID id;

    public GetPlan(String stringUuid) {
        this.id = UUID.fromString(stringUuid);
    }
}
