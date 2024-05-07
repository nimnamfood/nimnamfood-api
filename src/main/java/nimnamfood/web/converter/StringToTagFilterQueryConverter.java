package nimnamfood.web.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StringToTagFilterQueryConverter implements Converter<String, TagFilterQuery> {
    @Override
    @NonNull
    public TagFilterQuery convert(@NonNull String source) {
        final Set<TagFilter<?>> filters = Arrays.stream(source.split(","))
                .filter(part -> !part.isEmpty())
                .map(StringToTagFilterQueryConverter::toFilter)
                .collect(Collectors.toUnmodifiableSet());
        return new TagFilterQuery(filters);
    }

    private static TagFilter<?> toFilter(String s) {
        return switch (s.charAt(0)) {
            case '!' -> new DoesNotHaveTag(s.substring(1));
            case '(' -> new HasOneOfTags(Arrays.stream(s.substring(1, s.length() - 1).split("\\|"))
                    .collect(Collectors.toUnmodifiableSet()));
            default -> new HasTag(s);
        };
    }
}
