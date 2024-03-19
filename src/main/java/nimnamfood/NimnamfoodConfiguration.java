package nimnamfood;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import nimnamfood.infrastructure.repository.memory.MemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.tag.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
@ComponentScan("vtertre.command")
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
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CommandBus commandBus(
            List<CommandMiddleware> middlewares,
            Set<CommandHandler<?, ?>> commandHandlers,
            @Qualifier("Computation") ExecutorService executorService) {
        return new CommandBusAsync(middlewares, commandHandlers, executorService);
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

    @Bean
    @ConditionalOnProperty("nimnamfood.data.bootstrap")
    public CommandLineRunner commandLineRunner() {
        return args -> {
            final List<Tag> tags = List.of(
                    new Tag("rapide"),
                    new Tag("végé"),
                    new Tag("poisson gras"),
                    new Tag("poulet"),
                    new Tag("boeuf"),
                    new Tag("poisson maigre"),
                    new Tag("crustacés"),
                    new Tag("steph"),
                    new Tag("vincent"),
                    new Tag("soupe")
            );

            final List<Ingredient> ingredients = List.of(
                    new Ingredient("pavé de saumon", IngredientUnit.PIECE),
                    new Ingredient("cacao", IngredientUnit.GRAM),
                    new Ingredient("jus de citron", IngredientUnit.MILLILITER),
                    new Ingredient("sel", IngredientUnit.PINCH),
                    new Ingredient("sauce soja salée", IngredientUnit.TEASPOON),
                    new Ingredient("sirop d'érable", IngredientUnit.TABLESPOON),
                    new Ingredient("farine de riz", IngredientUnit.GRAM),
                    new Ingredient("gousse d'ail", IngredientUnit.PIECE),
                    new Ingredient("crevettes", IngredientUnit.GRAM),
                    new Ingredient("riz", IngredientUnit.GRAM),
                    new Ingredient("chili flakes", IngredientUnit.PINCH),
                    new Ingredient("patate douce", IngredientUnit.GRAM)
            );

            tags.forEach(tag -> Repositories.tags().add(tag));
            ingredients.forEach(ingredient -> Repositories.ingredients().add(ingredient));
        };
    }
}
