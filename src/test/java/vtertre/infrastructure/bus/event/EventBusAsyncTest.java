package vtertre.infrastructure.bus.event;

import com.google.common.util.concurrent.MoreExecutors;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import vtertre.ddd.event.DomainEvent;
import vtertre.ddd.event.EventBusMiddleware;
import vtertre.ddd.event.EventCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class EventBusAsyncTest {
    ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    @Test
    void anEventCanHaveNoCaptors() {
        EventBusAsync bus = new EventBusAsync(Collections.emptySet(), Collections.emptySet(), executorService);

        assertThatNoException().isThrownBy(() -> bus.publish(List.of(new FakeEvent())));
    }

    @Test
    void triggersAllCaptors() {
        FakeEventCaptor captor1 = new FakeEventCaptor();
        FakeEventCaptor captor2 = new FakeEventCaptor();
        EventBusAsync bus = new EventBusAsync(Collections.emptySet(), Set.of(captor2, captor1), executorService);
        FakeEvent event = new FakeEvent();

        bus.publish(List.of(event));

        assertThat(captor1.called).isTrue();
        assertThat(captor1.event).isEqualTo(event);
        assertThat(captor2.called).isTrue();
        assertThat(captor2.event).isEqualTo(event);
    }

    @Test
    void chainsTheMiddlewaresInOrder() {
        List<EventBusMiddleware> callChain = Lists.newArrayList();
        FakeMiddleware middleware1 = new FakeMiddleware(callChain);
        FakeMiddleware middleware2 = new FakeMiddleware(callChain);
        FakeEventCaptor captor = new FakeEventCaptor();
        EventBusAsync bus = new EventBusAsync(Sets.newLinkedHashSet(middleware1, middleware2), Set.of(captor), executorService);
        FakeEvent event = new FakeEvent();

        bus.publish(List.of(event));

        assertThat(middleware1.event).isEqualTo(event);
        assertThat(middleware2.event).isEqualTo(event);
        assertThat(callChain).containsExactly(middleware1, middleware2);
        assertThat(captor.called).isTrue();
    }

    private static class FakeEvent implements DomainEvent {
    }

    private static class FakeEventCaptor implements EventCaptor<FakeEvent> {
        boolean called;
        FakeEvent event;

        @Override
        public void execute(FakeEvent event) {
            this.called = true;
            this.event = event;
        }
    }

    private static class FakeMiddleware implements EventBusMiddleware {
        DomainEvent event;
        final List<EventBusMiddleware> callChain;

        FakeMiddleware(List<EventBusMiddleware> callChain) {
            this.callChain = callChain;
        }

        @Override
        public void intercept(DomainEvent event, Runnable next) {
            this.event = event;
            this.callChain.add(this);
            next.run();
        }
    }
}