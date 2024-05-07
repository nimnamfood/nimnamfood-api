package nimnamfood.query.recipe;

import nimnamfood.web.converter.TagFilterQuery;

public interface TagQueryTranslator<T> {
    T toQueryValue(TagFilterQuery query);
}
