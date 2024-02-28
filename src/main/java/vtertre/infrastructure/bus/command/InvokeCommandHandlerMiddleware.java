package vtertre.infrastructure.bus.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vtertre.command.Command;
import vtertre.command.CommandBus;
import vtertre.command.CommandHandler;
import vtertre.command.CommandMiddleware;
import vtertre.infrastructure.bus.MessageHandler;
import vtertre.infrastructure.bus.NoHandlerFound;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InvokeCommandHandlerMiddleware implements CommandMiddleware {
    private final Map<Class<?>, CommandHandler<?, ?>> handlers;
    private final ExecutorService executorService;
    private final static Logger LOGGER = LoggerFactory.getLogger(InvokeCommandHandlerMiddleware.class);

    public InvokeCommandHandlerMiddleware(Set<CommandHandler<?, ?>> handlers, ExecutorService executorService) {
        this.handlers = handlers.stream().collect(
                Collectors.toUnmodifiableMap(MessageHandler::messageType, handler -> handler));
        this.executorService = executorService;
    }

    @Override
    public <T> CompletableFuture<T> intercept(CommandBus commandBus, Command<T> command, Supplier<CompletableFuture<T>> nextMiddleware) {
        return Optional.ofNullable(this.handlers.get(command.getClass()))
                .map(handler -> (CommandHandler<Command<T>, T>) handler)
                .map(handler -> CompletableFuture.supplyAsync(() -> {
                    LOGGER.debug("Applying handler {}", handler.getClass());
                    return handler.execute(command);
                }, this.executorService))
                .orElseThrow(() -> new NoHandlerFound(command.getClass()));
    }
}
