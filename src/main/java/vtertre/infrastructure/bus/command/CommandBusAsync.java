package vtertre.infrastructure.bus.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vtertre.command.Command;
import vtertre.command.CommandBus;
import vtertre.command.CommandHandler;
import vtertre.command.CommandMiddleware;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class CommandBusAsync implements CommandBus {
    private final MiddlewareChainLink firstMiddlewareChainLink;
    private final static Logger LOGGER = LoggerFactory.getLogger(CommandBusAsync.class);

    public CommandBusAsync(Set<CommandMiddleware> middlewares, Set<CommandHandler<?, ?>> handlers, ExecutorService executorService) {
        MiddlewareChainLink currentLink = new MiddlewareChainLink(
                new InvokeCommandHandlerMiddleware(handlers, executorService),
                null
        );
        for (CommandMiddleware middleware : middlewares.stream().toList().reversed()) {
            currentLink = new MiddlewareChainLink(middleware, currentLink);
        }
        this.firstMiddlewareChainLink = currentLink;
    }

    @Override
    public <TResponse> CompletableFuture<TResponse> dispatch(Command<TResponse> command) {
        return this.firstMiddlewareChainLink.apply(command).thenApply(
                tuple -> tuple.apply((result, events) -> result));
    }

    private class MiddlewareChainLink {
        private final CommandMiddleware currentMiddleware;
        private final MiddlewareChainLink nextLink;

        MiddlewareChainLink(CommandMiddleware currentMiddleware, MiddlewareChainLink nextLink) {
            this.currentMiddleware = currentMiddleware;
            this.nextLink = nextLink;
        }

        public <T> CompletableFuture<Tuple<T, List<DomainEvent>>> apply(Command<T> command) {
            LOGGER.debug("Running middleware {}", this.currentMiddleware.getClass());
            return this.currentMiddleware.intercept(CommandBusAsync.this, command, () -> this.nextLink.apply(command));
        }
    }
}
