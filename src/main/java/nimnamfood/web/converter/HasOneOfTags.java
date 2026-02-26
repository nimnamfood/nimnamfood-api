package nimnamfood.web.converter;

import java.util.Set;

public record HasOneOfTags(Set<String> value) implements TagFilter<Set<String>> {}
