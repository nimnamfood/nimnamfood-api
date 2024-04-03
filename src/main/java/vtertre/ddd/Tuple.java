package vtertre.ddd;

import java.util.Objects;
import java.util.function.BiFunction;

public final class Tuple<T1, T2> {
    public final T1 _1;
    public final T2 _2;

    public Tuple(T1 t1, T2 t2) {
        this._1 = t1;
        this._2 = t2;
    }

    public static <T1, T2> Tuple<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple<>(t1, t2);
    }

    public <U1, U2> Tuple<U1, U2> map(BiFunction<? super T1, ? super T2, Tuple<U1, U2>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return mapper.apply(this._1, this._2);
    }

    public <U> U apply(BiFunction<? super T1, ? super T2, ? extends U> function) {
        Objects.requireNonNull(function, "function is null");
        return function.apply(this._1, this._2);
    }
}
