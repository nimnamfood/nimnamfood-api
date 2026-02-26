package nimnamfood.web.converter;

public sealed interface TagFilter<T> permits HasTag, DoesNotHaveTag, HasOneOfTags {
    T value();
}
