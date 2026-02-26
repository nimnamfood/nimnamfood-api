package nimnamfood.query.recipe;

import nimnamfood.web.converter.DoesNotHaveTag;
import nimnamfood.web.converter.HasOneOfTags;
import nimnamfood.web.converter.HasTag;
import nimnamfood.web.converter.TagFilterQuery;
import vtertre.ddd.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JdbcTagQueryTranslator implements TagQueryTranslator<Tuple<String, Map<String, Set<String>>>> {
    private final String jsonbTagsColumn;

    public JdbcTagQueryTranslator(String jsonbTagsColumn) {
        this.jsonbTagsColumn = jsonbTagsColumn;
    }

    @Override
    public Tuple<String, Map<String, Set<String>>> toQueryValue(TagFilterQuery query) {
        if (query.values().isEmpty()) {
            return null;
        }

        var clauses = new ArrayList<String>();
        var params = new HashMap<String, Set<String>>();

        var inclusiveFilterValues = new HashSet<String>();
        var exclusiveFilterValues = new HashSet<String>();
        var orFilters = new ArrayList<HasOneOfTags>();

        for (var filter : query.values()) {
            switch (filter) {
                case HasTag t         -> inclusiveFilterValues.add(t.value());
                case DoesNotHaveTag t -> exclusiveFilterValues.add(t.value());
                case HasOneOfTags t   -> orFilters.add(t);
            }
        }

        if (!inclusiveFilterValues.isEmpty()) {
            clauses.add("jsonb_exists_all(%s, array[:hasTagIds])".formatted(jsonbTagsColumn));
            params.put("hasTagIds", inclusiveFilterValues);
        }

        if (!exclusiveFilterValues.isEmpty()) {
            clauses.add("NOT (jsonb_exists_any(%s, array[:doesNotHaveTagIds]))".formatted(jsonbTagsColumn));
            params.put("doesNotHaveTagIds", exclusiveFilterValues);
        }

        for (int i = 0; i < orFilters.size(); i++) {
            var paramName = "hasOneOfTagIds" + i;
            clauses.add("jsonb_exists_any(%s, array[:%s])".formatted(jsonbTagsColumn, paramName));
            params.put(paramName, orFilters.get(i).value());
        }

        return Tuple.of(String.join(" AND ", clauses), params);
    }
}
