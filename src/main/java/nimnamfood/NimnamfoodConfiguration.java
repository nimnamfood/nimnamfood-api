package nimnamfood;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import nimnamfood.infrastructure.repository.jdbc.JdbcRepositories;
import nimnamfood.infrastructure.repository.memory.MemoryRepositories;
import nimnamfood.model.Repositories;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import vtertre.command.CommandBus;
import vtertre.command.CommandHandler;
import vtertre.command.CommandMiddleware;
import vtertre.command.CommandValidator;
import vtertre.ddd.event.EventBus;
import vtertre.ddd.event.EventBusMiddleware;
import vtertre.ddd.event.EventCaptor;
import vtertre.infrastructure.bus.command.CommandBusAsync;
import vtertre.infrastructure.bus.event.EventBusAsync;
import vtertre.infrastructure.bus.event.EventPublisherMiddleware;
import vtertre.infrastructure.bus.query.QueryBusAsync;
import vtertre.query.QueryBus;
import vtertre.query.QueryHandler;
import vtertre.query.QueryMiddleware;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Import({CommandValidator.class, EventPublisherMiddleware.class})
public class NimnamfoodConfiguration {
    @Bean
    @Qualifier("Computation")
    public ExecutorService fixedThreadPoolExecutorService() {
        return Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("computation-pool-%d").build()
        );
    }

    @Bean
    @Qualifier("Io")
    public ExecutorService virtualThreadPerTaskExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public EventBus eventBus(
            List<EventBusMiddleware> middlewares,
            Set<EventCaptor<?>> eventCaptors,
            @Qualifier("Io") ExecutorService executorService) {
        return new EventBusAsync(Sets.newLinkedHashSet(middlewares), eventCaptors, executorService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CommandBus commandBus(
            List<CommandMiddleware> middlewares,
            Set<CommandHandler<?, ?>> commandHandlers,
            @Qualifier("Computation") ExecutorService executorService) {
        return new CommandBusAsync(Sets.newLinkedHashSet(middlewares), commandHandlers, executorService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public QueryBus queryBus(
            List<QueryMiddleware> middlewares,
            Set<QueryHandler<?, ?>> queryHandlers,
            @Qualifier("Io") ExecutorService executorService) {
        return new QueryBusAsync(Sets.newLinkedHashSet(middlewares), queryHandlers, executorService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(value = "nimnamfood.data.inmemory", havingValue = "true")
    public Repositories memoryRepositories() {
        return new MemoryRepositories();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(value = "nimnamfood.data.inmemory", havingValue = "false", matchIfMissing = true)
    public Repositories jdbcRepositories() {
        return new JdbcRepositories();
    }
}
