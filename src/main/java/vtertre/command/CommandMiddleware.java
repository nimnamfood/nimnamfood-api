package vtertre.command;

import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface CommandMiddleware {
    <T> CompletableFuture<Tuple<T, List<DomainEvent>>> intercept(
            CommandBus commandBus, Command<T> command, Supplier<CompletableFuture<Tuple<T, List<DomainEvent>>>> nextMiddleware);
}
