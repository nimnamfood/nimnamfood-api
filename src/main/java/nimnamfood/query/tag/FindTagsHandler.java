package nimnamfood.query.tag;

import nimnamfood.query.tag.model.TagSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandlerJdbc;

import java.util.List;
import java.util.UUID;

@Component
public class FindTagsHandler extends QueryHandlerJdbc<FindTags, List<TagSummary>> {
    @Override
    protected List<TagSummary> execute(FindTags query, NamedParameterJdbcTemplate template) {
        final String baseSqlQuery = "SELECT * FROM tags";

        if (query.query == null || query.query.isEmpty()) {
            return template.query(baseSqlQuery, tagSummaryMapper());
        }

        return template.query(
                baseSqlQuery + " WHERE UNACCENT(name) ILIKE UNACCENT(:query)",
                new MapSqlParameterSource("query", "%" + query.query + "%"),
                tagSummaryMapper()
        );
    }

    private static RowMapper<TagSummary> tagSummaryMapper() {
        return (resultSet, rowNum) -> new TagSummary(
                resultSet.getObject("id", UUID.class), resultSet.getString("name"));
    }
}
