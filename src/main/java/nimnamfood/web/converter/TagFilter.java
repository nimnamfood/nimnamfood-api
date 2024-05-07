package nimnamfood.web.converter;

public abstract class TagFilter<T> {
    protected final T value;

    protected TagFilter(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }
}
