package vtertre.command;

import java.util.concurrent.CompletableFuture;

public interface CommandBus {
    <TResponse> CompletableFuture<TResponse> dispatch(Command<TResponse> command);
}
