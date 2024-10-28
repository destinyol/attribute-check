import java.io.Serializable;

@FunctionalInterface
public interface FieldGetter<T> extends Serializable {
    Object get(T source);
}
