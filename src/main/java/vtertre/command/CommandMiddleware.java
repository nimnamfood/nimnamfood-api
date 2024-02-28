package vtertre.command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface CommandMiddleware {
    <T> CompletableFuture<T> intercept(
            CommandBus commandBus, Command<T> command, Supplier<CompletableFuture<T>> nextMiddleware);
}
