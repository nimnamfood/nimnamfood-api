package vtertre.infrastructure.bus.command;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.Test;
import vtertre.command.Command;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;
import vtertre.infrastructure.bus.NoHandlerFound;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class InvokeCommandHandlerMiddlewareTest {
    ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    @Test
    void throwsAnExceptionWhenNoHandlerIsFound() {
        InvokeCommandHandlerMiddleware middleware = new InvokeCommandHandlerMiddleware(Collections.emptySet(), executorService);

        assertThatExceptionOfType(NoHandlerFound.class)
                .isThrownBy(() -> middleware.intercept(null, new FakeCommand(), null))
                .withMessage("NO_HANDLER_FOUND - class vtertre.infrastructure.bus.command.InvokeCommandHandlerMiddlewareTest$FakeCommand");
    }

    @Test
    void executesTheCommandWithTheHandler() throws Exception {
        FakeCommandHandler handler = new FakeCommandHandler();
        InvokeCommandHandlerMiddleware middleware = new InvokeCommandHandlerMiddleware(
                Sets.newHashSet(handler), executorService);
        FakeCommand command = new FakeCommand();

        CompletableFuture<Tuple<String, List<DomainEvent>>> result = middleware.intercept(null, command, null);

        assertThat(result.get()._1).isEqualTo("fake command result");
        assertThat(handler.command).isEqualTo(command);
        assertThat(((FakeEvent) result.get()._2.getFirst()).name).isEqualTo("test");
    }

    @Test
    void isATerminalChainMiddleware() throws Exception {
        FakeCommandHandler handler = new FakeCommandHandler();
        InvokeCommandHandlerMiddleware middleware = new InvokeCommandHandlerMiddleware(
                Sets.newHashSet(handler), executorService);
        FakeCommand command = new FakeCommand();
        FakeMiddleware fakeMiddleware = new FakeMiddleware();

        CompletableFuture<Tuple<String, List<DomainEvent>>> result = middleware.intercept(null, command, () -> fakeMiddleware.apply(command));

        assertThat(result.get()._1).isEqualTo("fake command result");
        assertThat(fakeMiddleware.called).isFalse();
    }

    private static class FakeCommand implements Command<String> {
    }

    private record FakeEvent(String name) implements DomainEvent {
    }

    private static class FakeCommandHandler implements CommandHandler<FakeCommand, String> {
        FakeCommand command;

        @Override
        public Tuple<String, List<DomainEvent>> execute(FakeCommand command) {
            this.command = command;
            return Tuple.of("fake command result", List.of(new FakeEvent("test")));
        }
    }

    private static class FakeMiddleware {
        boolean called = false;

        <T> CompletableFuture<Tuple<T, List<DomainEvent>>> apply(Command<T> command) {
            this.called = true;
            return CompletableFuture.completedFuture(Tuple.of(null, null));
        }
    }
}
