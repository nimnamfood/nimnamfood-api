package vtertre.infrastructure.bus.event;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vtertre.command.Command;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;
import vtertre.ddd.event.EventBus;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class EventPublisherMiddlewareTest {
    EventBus eventBus = Mockito.mock();

    @Test
    void publishesEventsAndPassesThrough() throws Exception {
        EventPublisherMiddleware middleware = new EventPublisherMiddleware(eventBus);
        FakeCommand command = new FakeCommand();
        List<DomainEvent> eventsToPublish = List.of(new FakeEvent());

        Tuple<String, List<DomainEvent>> result = middleware.intercept(
                null, command, () -> CompletableFuture.completedFuture(Tuple.of("result", eventsToPublish))).get();

        Mockito.verify(eventBus, Mockito.times(1)).publish(eventsToPublish);
        assertThat(result._1).isEqualTo("result");
        assertThat(result._2).isEqualTo(eventsToPublish);
    }

    private static class FakeEvent implements DomainEvent {
    }

    private static class FakeCommand implements Command<String> {
    }
}