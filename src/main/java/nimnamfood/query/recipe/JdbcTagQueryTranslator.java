package nimnamfood.query.recipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import nimnamfood.web.converter.DoesNotHaveTag;
import nimnamfood.web.converter.HasOneOfTags;
import nimnamfood.web.converter.HasTag;
import nimnamfood.web.converter.TagFilterQuery;
import vtertre.ddd.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        final List<String> clauses = Lists.newArrayList();
        final Map<String, Set<String>> params = Maps.newHashMap();

        final Set<String> inclusiveFilterValues = query.values().stream()
                .filter(filter -> filter.getClass().equals(HasTag.class))
                .map(filter -> ((HasTag) filter).value())
                .collect(Collectors.toUnmodifiableSet());
        if (!inclusiveFilterValues.isEmpty()) {
            clauses.add("jsonb_exists_all(" + this.jsonbTagsColumn + ", array[:hasTagIds])");
            params.put("hasTagIds", inclusiveFilterValues);
        }


        final Set<String> exclusiveFilterValues = query.values().stream()
                .filter(filter -> filter.getClass().equals(DoesNotHaveTag.class))
                .map(filter -> ((DoesNotHaveTag) filter).value())
                .collect(Collectors.toUnmodifiableSet());
        if (!exclusiveFilterValues.isEmpty()) {
            clauses.add("NOT (jsonb_exists_any(" + this.jsonbTagsColumn + ", array[:doesNotHaveTagIds]))");
            params.put("doesNotHaveTagIds", exclusiveFilterValues);
        }

        final List<HasOneOfTags> orFilters = query.values().stream()
                .filter(filter -> filter.getClass().equals(HasOneOfTags.class))
                .map(filter -> (HasOneOfTags) filter)
                .toList();
        if (!orFilters.isEmpty()) {
            IntStream.range(0, orFilters.size()).forEach(index -> {
                final String paramName = "hasOneOfTagIds" + index;
                clauses.add("jsonb_exists_any(" + this.jsonbTagsColumn + ", array[:" + paramName + "])");
                params.put(paramName, orFilters.get(index).value());
            });
        }

        return Tuple.of(String.join(" AND ", clauses), params);
    }
}
