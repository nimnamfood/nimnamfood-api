package vtertre.infrastructure.bus.query;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import vtertre.infrastructure.bus.NoHandlerFound;
import vtertre.query.Query;
import vtertre.query.QueryHandler;
import vtertre.query.QueryMiddleware;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class QueryBusAsyncTest {

    ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    @Test
    void throwsAnExceptionWhenNoHandlerIsFound() {
        QueryBusAsync bus = new QueryBusAsync(Collections.emptySet(), Collections.emptySet(), executorService);

        CompletableFuture<String> result = bus.dispatch(new FakeQuery());

        assertThatExceptionOfType(ExecutionException.class)
                .isThrownBy(result::get)
                .havingCause()
                .isInstanceOf(NoHandlerFound.class)
                .withMessage("NO_HANDLER_FOUND - class vtertre.infrastructure.bus.query.QueryBusAsyncTest$FakeQuery");
    }

    @Test
    void executesTheQueryWithTheHandler() throws Exception {
        FakeQueryHandler handler = new FakeQueryHandler();
        QueryBusAsync bus = new QueryBusAsync(Collections.emptySet(), Sets.newHashSet(handler), executorService);
        FakeQuery query = new FakeQuery();

        CompletableFuture<String> result = bus.dispatch(query);

        assertThat(result.get()).isEqualTo("fake query result");
        assertThat(handler.query).isEqualTo(query);
    }

    @Test
    void chainsTheMiddlewaresInOrder() throws Exception {
        FakeQueryHandler handler = new FakeQueryHandler();
        List<QueryMiddleware> callChain = Lists.newArrayList();
        FakeMiddleware firstMiddleware = new FakeMiddleware(callChain);
        FakeMiddleware secondMiddleware = new FakeMiddleware(callChain);
        QueryBusAsync bus = new QueryBusAsync(
                Sets.newLinkedHashSet(List.of(firstMiddleware, secondMiddleware)), Sets.newHashSet(handler), executorService);
        FakeQuery query = new FakeQuery();

        CompletableFuture<String> result = bus.dispatch(query);

        assertThat(firstMiddleware.called).isTrue();
        assertThat(secondMiddleware.called).isTrue();
        assertThat(callChain).containsExactly(firstMiddleware, secondMiddleware);
        assertThat(result.get()).isEqualTo("fake query result");
    }

    private static class FakeQuery extends Query<String> {
    }

    private static class FakeQueryHandler implements QueryHandler<FakeQuery, String> {
        FakeQuery query;

        @Override
        public String execute(FakeQuery query) {
            this.query = query;
            return "fake query result";
        }
    }

    private static class FakeMiddleware implements QueryMiddleware {
        boolean called;
        final List<QueryMiddleware> callChain;

        FakeMiddleware(List<QueryMiddleware> callChain) {
            this.callChain = callChain;
        }

        @Override
        public <T> T intercept(Query<T> query, Supplier<T> nextMiddleware) {
            this.called = true;
            this.callChain.add(this);
            return nextMiddleware.get();
        }
    }
}
