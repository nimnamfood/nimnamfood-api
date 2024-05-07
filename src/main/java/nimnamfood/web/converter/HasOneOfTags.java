package nimnamfood.web.converter;

import java.util.Set;

public class HasOneOfTags extends TagFilter<Set<String>> {
    public HasOneOfTags(Set<String> value) {
        super(value);
    }
}
