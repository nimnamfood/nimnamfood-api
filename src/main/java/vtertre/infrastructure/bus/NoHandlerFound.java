package vtertre.infrastructure.bus;

public class NoHandlerFound extends RuntimeException {

    public NoHandlerFound(Class<?> aClass) {
        super(String.format("NO_HANDLER_FOUND - %s", aClass));
    }
}
