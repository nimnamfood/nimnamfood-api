package nimnamfood.query.tag;

import nimnamfood.query.tag.model.TagSummary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandlerJdbc;

import java.util.List;

@Component
public class FindTagsHandler extends QueryHandlerJdbc<FindTags, List<TagSummary>> {
    @Override
    protected List<TagSummary> execute(FindTags query, NamedParameterJdbcTemplate template) {
        final String baseSqlQuery = "SELECT * FROM tags";
        final TagSummary.Mapper mapper = TagSummary.mapper();

        if (query.query == null || query.query.isEmpty()) {
            return template.query(baseSqlQuery, mapper);
        }

        return template.query(
                baseSqlQuery + " WHERE name ILIKE :query",
                new MapSqlParameterSource("query", "%" + query.query + "%"),
                mapper
        );
    }
}
