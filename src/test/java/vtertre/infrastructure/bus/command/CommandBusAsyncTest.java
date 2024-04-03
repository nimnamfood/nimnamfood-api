package vtertre.infrastructure.bus.command;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import vtertre.command.Command;
import vtertre.command.CommandBus;
import vtertre.command.CommandHandler;
import vtertre.command.CommandMiddleware;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;
import vtertre.infrastructure.bus.NoHandlerFound;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CommandBusAsyncTest {
    ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    @Test
    void throwsAnExceptionWhenNoHandlerIsFound() {
        CommandBusAsync bus = new CommandBusAsync(Collections.emptySet(), Collections.emptySet(), executorService);

        assertThatExceptionOfType(NoHandlerFound.class)
                .isThrownBy(() -> bus.dispatch(new FakeCommand()))
                .withMessage("NO_HANDLER_FOUND - class vtertre.infrastructure.bus.command.CommandBusAsyncTest$FakeCommand");
    }

    @Test
    void executesTheCommandWithTheHandler() throws Exception {
        FakeCommandHandler handler = new FakeCommandHandler();
        CommandBusAsync bus = new CommandBusAsync(Collections.emptySet(), Sets.newHashSet(handler), executorService);
        FakeCommand command = new FakeCommand();

        CompletableFuture<String> result = bus.dispatch(command);

        assertThat(result.get()).isEqualTo("fake command result");
        assertThat(handler.command).isEqualTo(command);
    }

    @Test
    void chainsTheMiddlewaresInOrder() throws Exception {
        FakeCommandHandler handler = new FakeCommandHandler();
        List<CommandMiddleware> callChain = Lists.newArrayList();
        FakeMiddleware firstMiddleware = new FakeMiddleware(callChain);
        FakeMiddleware secondMiddleware = new FakeMiddleware(callChain);
        CommandBusAsync bus = new CommandBusAsync(
                Sets.newLinkedHashSet(List.of(firstMiddleware, secondMiddleware)), Sets.newHashSet(handler), executorService);
        FakeCommand command = new FakeCommand();

        CompletableFuture<String> result = bus.dispatch(command);

        assertThat(firstMiddleware.called).isTrue();
        assertThat(secondMiddleware.called).isTrue();
        assertThat(callChain).containsExactly(firstMiddleware, secondMiddleware);
        assertThat(result.get()).isEqualTo("fake command result");
    }

    private static class FakeCommand implements Command<String> {
    }

    private static class FakeCommandHandler implements CommandHandler<FakeCommand, String> {
        FakeCommand command;

        @Override
        public Tuple<String, List<DomainEvent>> execute(FakeCommand command) {
            this.command = command;
            return Tuple.of("fake command result", Collections.emptyList());
        }
    }

    private static class FakeMiddleware implements CommandMiddleware {
        boolean called;
        final List<CommandMiddleware> callChain;

        FakeMiddleware(List<CommandMiddleware> callChain) {
            this.callChain = callChain;
        }

        @Override
        public <T> CompletableFuture<Tuple<T, List<DomainEvent>>> intercept(CommandBus commandBus, Command<T> command, Supplier<CompletableFuture<Tuple<T, List<DomainEvent>>>> nextMiddleware) {
            this.called = true;
            this.callChain.add(this);
            return nextMiddleware.get();
        }
    }
}
