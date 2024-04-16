package vtertre.infrastructure.bus.event;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class EventBusAsync implements EventBus {
    private final ExecutorService directExecutorService = MoreExecutors.newDirectExecutorService();
    private final ExecutorService executorService;
    private final List<EventCaptor<?>> captors;
    private final MiddlewareChainLink firstMiddlewareChainLink;

    private final static Logger LOGGER = LoggerFactory.getLogger(EventBusAsync.class);

    public EventBusAsync(Set<EventBusMiddleware> middlewares, Set<EventCaptor<?>> captors, ExecutorService executorService) {
        this.executorService = executorService;
        this.captors = ImmutableList.copyOf(captors);

        MiddlewareChainLink currentLink = new CaptorInvocation();
        for (EventBusMiddleware middleware : middlewares.stream().toList().reversed()) {
            currentLink = new MiddlewareChainLink(middleware, currentLink);
        }
        this.firstMiddlewareChainLink = currentLink;
    }

    @Override
    public <T extends DomainEvent> void publish(List<T> events) {
        events.stream()
                .map(event -> Tuple.of(event, this.captors.stream()
                        .filter(captor -> captor.eventType().equals(event.getClass()))
                        .map(captor -> (EventCaptor<T>) captor)
                        .toList()))
                .toList()
                .forEach(tuple -> tuple._2.forEach(captor -> execute(tuple._1, captor)));
    }

    private <T extends DomainEvent> CompletableFuture<Boolean> execute(T event, EventCaptor<T> captor) {
        final ExecutorService executor = captor.getClass().getAnnotation(Synced.class) != null ? this.directExecutorService : this.executorService;
        return CompletableFuture.supplyAsync(() -> firstMiddlewareChainLink.apply(captor, event), executor);
    }

    private static class MiddlewareChainLink {
        private final EventBusMiddleware middleware;
        private final MiddlewareChainLink nextLink;

        MiddlewareChainLink(EventBusMiddleware middleware, MiddlewareChainLink nextLink) {
            this.middleware = middleware;
            this.nextLink = nextLink;
        }

        public <T extends DomainEvent> boolean apply(EventCaptor<T> captor, T event) {
            LOGGER.debug("Running middleware {}", middleware.getClass());
            middleware.intercept(event, () -> nextLink.apply(captor, event));
            return true;
        }
    }

    private static class CaptorInvocation extends MiddlewareChainLink {
        public CaptorInvocation() {
            super(null, null);
        }

        @Override
        public <T extends DomainEvent> boolean apply(EventCaptor<T> captor, T event) {
            LOGGER.debug("Applying captor {}", captor.getClass());
            captor.execute(event);
            return true;
        }
    }
}
