package nimnamfood.web.converter;

import java.util.Set;

public record TagFilterQuery(Set<TagFilter<?>> values) {
    public boolean isEmpty() {
        return this.values.isEmpty();
    }
}
