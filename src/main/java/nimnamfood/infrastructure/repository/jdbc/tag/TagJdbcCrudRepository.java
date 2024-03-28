package nimnamfood.infrastructure.repository.jdbc.tag;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface TagJdbcCrudRepository extends CrudRepository<TagDbo, UUID> {
}
