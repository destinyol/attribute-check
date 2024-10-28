
public class ValueRuntimeException extends RuntimeException{

    private static final long serialVersionUID = -5561631295910836458L;
    private Object obj;

    public ValueRuntimeException() {
    }

    public ValueRuntimeException(Object value) {
        this.obj = value;
    }

    public <T> T getValue() {
        return (T) this.obj;
    }

    public void setValue(Object value) {
        this.obj = value;
    }

}
