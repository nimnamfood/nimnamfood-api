package nimnamfood;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import nimnamfood.infrastructure.repository.memory.MemoryRepositories;
import nimnamfood.model.Repositories;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import vtertre.command.CommandBus;
import vtertre.command.CommandHandler;
import vtertre.command.CommandMiddleware;
import vtertre.infrastructure.bus.command.CommandBusAsync;
import vtertre.infrastructure.bus.query.QueryBusAsync;
import vtertre.query.QueryBus;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class NimnamfoodConfiguration {
    @Qualifier("Computation")
    @Bean
    public ExecutorService fixedThreadPoolExecutorService() {
        return Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("computation-pool-%d").build()
        );
    }

    @Qualifier("Io")
    @Bean
    public ExecutorService cachedThreadPoolExecutorService() {
        return Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("io-pool-%d").build());
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CommandBus commandBus(
            // @Order does not seem to work on Sets
            List<CommandMiddleware> middlewares,
            Set<CommandHandler<?, ?>> commandHandlers,
            @Qualifier("Computation") ExecutorService executorService) {
        return new CommandBusAsync(Sets.newHashSet(middlewares), commandHandlers, executorService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public QueryBus queryBus(
            Set<QueryHandler<?, ?>> queryHandlers,
            @Qualifier("Io") ExecutorService executorService) {
        return new QueryBusAsync(queryHandlers, executorService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Repositories repositories() {
        return new MemoryRepositories();
    }
}
