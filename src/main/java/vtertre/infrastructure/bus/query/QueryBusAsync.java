package vtertre.infrastructure.bus.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vtertre.infrastructure.bus.NoHandlerFound;
import vtertre.query.Query;
import vtertre.query.QueryBus;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class QueryBusAsync implements QueryBus {
    private final List<QueryHandler<?, ?>> queryHandlers;
    private final ExecutorService executorService;
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryBusAsync.class);

    public QueryBusAsync(Set<QueryHandler<?, ?>> queryHandlers, ExecutorService executorService) {
        this.queryHandlers = queryHandlers.stream().toList();
        this.executorService = executorService;
    }

    @Override
    public <TResponse> CompletableFuture<TResponse> dispatch(Query<TResponse> query) {
        return this.queryHandlers
                .stream()
                .filter(queryHandler -> queryHandler.messageType().equals(query.getClass()))
                .findFirst()
                .map(queryHandler -> (QueryHandler<Query<TResponse>, TResponse>) queryHandler)
                .map(queryHandler -> this.execute(queryHandler, query))
                .orElseGet(() -> CompletableFuture.failedFuture(new NoHandlerFound(query.getClass())));
    }

    private <TResponse> CompletableFuture<TResponse> execute(
            QueryHandler<Query<TResponse>, TResponse> handler,
            Query<TResponse> query
    ) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.debug("Applying handler {}", handler.getClass());
            return handler.execute(query);
        }, this.executorService);
    }
}
