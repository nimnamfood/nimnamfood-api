package nimnamfood.query.tag;

import nimnamfood.model.tag.Tag;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TagsViewTestHelper {
    @Autowired
    NamedParameterJdbcTemplate template;
    @Autowired
    JdbcClient client;

    public void insertTags(Tag... tags) {
        this.template.batchUpdate(
                "insert into view_tags values (:id, :name)",
                Arrays.stream(tags).map(i -> Map.of(
                        "id", i.getId(),
                        "name", i.getName()
                )).toArray(Map[]::new)
        );
    }

    public Optional<TagSummary> findTag(UUID id) {
        return this.client
                .sql("select * from view_tags where id = :id")
                .param("id", id)
                .query((resultSet, rowNum) -> new TagSummary(resultSet.getObject("id", UUID.class),
                        resultSet.getString("name")))
                .optional();
    }
}
