package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.TagRepository;
import nimnamfood.model.Repositories;

public class MemoryRepositories extends Repositories {
    private final TagMemoryRepository tagMemoryRepository = new TagMemoryRepository();

    @Override
    protected TagRepository getTags() {
        return tagMemoryRepository;
    }
}
