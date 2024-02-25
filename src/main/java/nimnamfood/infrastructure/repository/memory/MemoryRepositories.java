package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.CriteriaRepository;
import nimnamfood.model.Repositories;

public class MemoryRepositories extends Repositories {
    private final CriteriaMemoryRepository criteriaMemoryRepository = new CriteriaMemoryRepository();

    @Override
    protected CriteriaRepository getCriteria() {
        return criteriaMemoryRepository;
    }
}
