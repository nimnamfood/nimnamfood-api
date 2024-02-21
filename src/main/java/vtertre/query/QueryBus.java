package vtertre.query;

import java.util.concurrent.CompletableFuture;

public interface QueryBus {
    <TResponse> CompletableFuture<TResponse> send(Query<TResponse> query);
}
