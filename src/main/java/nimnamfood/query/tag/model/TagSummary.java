package nimnamfood.query.tag.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class TagSummary {
    public UUID id;
    public String name;

    public static Mapper mapper() {
        return new Mapper();
    }

    public static class Mapper implements RowMapper<TagSummary> {
        private Mapper() {
        }

        @Override
        public TagSummary mapRow(ResultSet result, int rowNum) throws SQLException {
            final TagSummary summary = new TagSummary();
            summary.id = result.getObject("id", UUID.class);
            summary.name = result.getString("name");
            return summary;
        }
    }
}
