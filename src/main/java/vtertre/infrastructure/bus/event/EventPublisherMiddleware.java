package vtertre.infrastructure.bus.event;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import vtertre.command.Command;
import vtertre.command.CommandBus;
import vtertre.command.CommandMiddleware;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;
import vtertre.ddd.event.EventBus;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Component
@Order(2)
public class EventPublisherMiddleware implements CommandMiddleware {
    private final EventBus eventBus;

    public EventPublisherMiddleware(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public <T> CompletableFuture<Tuple<T, List<DomainEvent>>> intercept(
            CommandBus commandBus, Command<T> command, Supplier<CompletableFuture<Tuple<T, List<DomainEvent>>>> nextMiddleware) {
        return nextMiddleware.get().thenApply(tuple -> {
            this.eventBus.publish(tuple.apply((result, events) -> events));
            return tuple;
        });
    }
}
