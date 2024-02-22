package vtertre.infrastructure.bus.query;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.Test;
import vtertre.query.Query;
import vtertre.query.QueryHandler;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class QueryBusAsyncTest {

    ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    @Test
    void throwsAnExceptionWhenNoHandlerIsFound() {
        QueryBusAsync bus = new QueryBusAsync(Collections.emptySet(), executorService);

        CompletableFuture<String> result = bus.send(new FakeQuery());

        assertThatExceptionOfType(ExecutionException.class)
                .isThrownBy(result::get)
                .havingCause()
                .isInstanceOf(RuntimeException.class)
                .withMessage("NO_HANDLER_FOUND - class vtertre.infrastructure.bus.query.QueryBusAsyncTest$FakeQuery");
    }

    @Test
    void executesTheQueryWithTheHandler() throws Exception {
        FakeQueryHandler handler = new FakeQueryHandler();
        QueryBusAsync bus = new QueryBusAsync(Sets.newHashSet(handler), executorService);
        FakeQuery query = new FakeQuery();

        CompletableFuture<String> result = bus.send(query);

        assertThat(result.get()).isEqualTo("fake query result");
        assertThat(handler.query).isEqualTo(query);
    }

    private static class FakeQuery implements Query<String> {
    }

    private static class FakeQueryHandler implements QueryHandler<FakeQuery, String> {
        FakeQuery query;

        @Override
        public String execute(FakeQuery query) {
            this.query = query;
            return "fake query result";
        }
    }
}
