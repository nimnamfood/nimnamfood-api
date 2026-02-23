package vtertre.infrastructure.bus.query;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vtertre.infrastructure.bus.NoHandlerFound;
import vtertre.query.Query;
import vtertre.query.QueryBus;
import vtertre.query.QueryHandler;
import vtertre.query.QueryMiddleware;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class QueryBusAsync implements QueryBus {
    private final ExecutorService executorService;
    private final MiddlewareChainLink firstMiddlewareChainLink;

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryBusAsync.class);

    public QueryBusAsync(Set<QueryMiddleware> middlewares, Set<QueryHandler<?, ?>> queryHandlers, ExecutorService executorService) {
        this.executorService = executorService;

        MiddlewareChainLink currentLink = new HandlerInvocation(ImmutableList.copyOf(queryHandlers));
        for (QueryMiddleware middleware : middlewares.stream().toList().reversed()) {
            currentLink = new MiddlewareChainLink(middleware, currentLink);
        }
        this.firstMiddlewareChainLink = currentLink;
    }

    @Override
    public <TResponse> CompletableFuture<TResponse> dispatch(Query<TResponse> query) {
        return CompletableFuture.supplyAsync(() -> this.firstMiddlewareChainLink.apply(query), this.executorService);
    }

    private static class MiddlewareChainLink {
        private final QueryMiddleware middleware;
        private final MiddlewareChainLink nextLink;

        MiddlewareChainLink(QueryMiddleware middleware, MiddlewareChainLink nextLink) {
            this.middleware = middleware;
            this.nextLink = nextLink;
        }

        public <T> T apply(Query<T> query) {
            LOGGER.debug("Running middleware {}", middleware.getClass());
            return middleware.intercept(query, () -> nextLink.apply(query));
        }
    }

    private static class HandlerInvocation extends MiddlewareChainLink {
        private final List<QueryHandler<?, ?>> handlers;

        HandlerInvocation(List<QueryHandler<?, ?>> handlers) {
            super(null, null);
            this.handlers = handlers;
        }

        @Override
        public <T> T apply(Query<T> query) {
            return this.handlers
                    .stream()
                    .filter(handler -> handler.messageType().equals(query.getClass()))
                    .map(handler -> {
                        LOGGER.debug("Applying handler {}", handler.getClass());
                        return ((QueryHandler<Query<T>, T>) handler).execute(query);
                    })
                    .findFirst()
                    .orElseThrow(() -> new NoHandlerFound(query.getClass()));
        }
    }
}
