package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.plan.Plan;
import nimnamfood.model.plan.PlanRepository;
import vtertre.infrastructure.persistence.memory.MemoryRepositoryWithUuid;

public class PlanMemoryRepository extends MemoryRepositoryWithUuid<Plan> implements PlanRepository {
}
