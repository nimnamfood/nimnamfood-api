package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.tag.Tag;
import nimnamfood.model.tag.TagRepository;
import vtertre.infrastructure.persistence.memory.MemoryRepositoryWithUuid;

public class TagMemoryRepository extends MemoryRepositoryWithUuid<Tag> implements TagRepository {
}
