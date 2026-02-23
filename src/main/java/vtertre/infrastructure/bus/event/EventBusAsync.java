package vtertre.infrastructure.bus.event;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vtertre.ddd.event.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class EventBusAsync implements EventBus {
    private final ExecutorService directExecutorService = MoreExecutors.newDirectExecutorService();
    private final ExecutorService executorService;
    private final MiddlewareChainLink firstMiddlewareChainLink;

    private final static Logger LOGGER = LoggerFactory.getLogger(EventBusAsync.class);

    public EventBusAsync(Set<EventBusMiddleware> middlewares, Set<EventCaptor<?>> captors, ExecutorService executorService) {
        this.executorService = executorService;

        MiddlewareChainLink currentLink = new CaptorInvocation(ImmutableList.copyOf(captors));
        for (EventBusMiddleware middleware : middlewares.stream().toList().reversed()) {
            currentLink = new MiddlewareChainLink(middleware, currentLink);
        }
        this.firstMiddlewareChainLink = currentLink;
    }

    @Override
    public <T extends DomainEvent> void publish(List<T> events) {
        events.forEach(this::execute);
    }

    private <T extends DomainEvent> CompletableFuture<Boolean> execute(T event) {
        final ExecutorService executor = event.getClass().getAnnotation(Synced.class) != null ? this.directExecutorService : this.executorService;
        return CompletableFuture.supplyAsync(() -> firstMiddlewareChainLink.apply(event), executor);
    }

    private static class MiddlewareChainLink {
        private final EventBusMiddleware middleware;
        private final MiddlewareChainLink nextLink;

        MiddlewareChainLink(EventBusMiddleware middleware, MiddlewareChainLink nextLink) {
            this.middleware = middleware;
            this.nextLink = nextLink;
        }

        public <T extends DomainEvent> boolean apply(T event) {
            LOGGER.debug("Running middleware {}", middleware.getClass());
            middleware.intercept(event, () -> nextLink.apply(event));
            return true;
        }
    }

    private static class CaptorInvocation extends MiddlewareChainLink {
        private final List<EventCaptor<?>> captors;

        public CaptorInvocation(List<EventCaptor<?>> captors) {
            super(null, null);
            this.captors = captors;
        }

        @Override
        public <T extends DomainEvent> boolean apply(T event) {
            return this.captors
                    .stream()
                    .filter(captor -> captor.eventType().equals(event.getClass()))
                    .map(captor -> {
                        LOGGER.debug("Applying captor {}", captor.getClass());
                        ((EventCaptor<T>) captor).execute(event);
                        return true;
                    })
                    .reduce((a, b) -> a && b)
                    .orElseThrow();
        }
    }
}
