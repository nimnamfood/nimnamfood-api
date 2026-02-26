package nimnamfood.web.converter;

public record DoesNotHaveTag(String value) implements TagFilter<String> {}
