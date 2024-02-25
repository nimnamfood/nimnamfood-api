package nimnamfood;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import nimnamfood.infrastructure.repository.memory.MemoryRepositories;
import nimnamfood.model.Repositories;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import vtertre.infrastructure.bus.query.QueryBusAsync;
import vtertre.query.QueryBus;
import vtertre.query.QueryHandler;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class NimnamfoodConfiguration {
    @Bean
    public ExecutorService cachedThreadPoolExecutorService() {
        return Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("io-pool-%d").build());
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public QueryBus queryBus(Set<QueryHandler<?, ?>> queryHandlers, ExecutorService executorService) {
        return new QueryBusAsync(queryHandlers, executorService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Repositories repositories() {
        return new MemoryRepositories();
    }
}
