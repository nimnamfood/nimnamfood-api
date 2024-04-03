package vtertre.ddd.event;

public interface EventBusMiddleware {
    void intercept(DomainEvent event, Runnable next);
}
