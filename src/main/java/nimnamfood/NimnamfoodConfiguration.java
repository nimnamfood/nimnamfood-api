package nimnamfood;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import nimnamfood.infrastructure.repository.memory.MemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
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
import vtertre.ddd.BaseEntity;
import vtertre.infrastructure.bus.command.CommandBusAsync;
import vtertre.infrastructure.bus.query.QueryBusAsync;
import vtertre.query.QueryBus;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
                    new Ingredient("pavé(s) de saumon", IngredientUnit.PIECE),
                    new Ingredient("cacao", IngredientUnit.GRAM),
                    new Ingredient("feuille(s) d'agar-agar", IngredientUnit.PIECE),
                    new Ingredient("jus de citron", IngredientUnit.MILLILITER),
                    new Ingredient("sel", IngredientUnit.PINCH),
                    new Ingredient("sauce soja salée", IngredientUnit.TEASPOON),
                    new Ingredient("sirop d'érable", IngredientUnit.TABLESPOON),
                    new Ingredient("farine de riz", IngredientUnit.GRAM),
                    new Ingredient("olive(s) verte(s)", IngredientUnit.GRAM),
                    new Ingredient("gousse(s) d'ail", IngredientUnit.PIECE),
                    new Ingredient("crevettes", IngredientUnit.GRAM),
                    new Ingredient("riz", IngredientUnit.GRAM),
                    new Ingredient("chili flakes", IngredientUnit.PINCH),
                    new Ingredient("patate douce", IngredientUnit.GRAM),
                    new Ingredient("huile d'olive", IngredientUnit.GRAM)
            );

            final Recipe recipe = Recipe.factory().create("Recette test", 2,
                    ingredients.stream().map(ingredient -> new RecipeIngredient(
                            ingredient.getId(), 10, ingredient.getUnit(), ingredient.getName().equals("sel"))).collect(Collectors.toSet()),
                    "Une première étape assez courte.\n\nUne deuxième étape beaucoup plus longue pour pouvoir tester le fait que les lignes s'affichent correctement en sorti et ce même sur des écrans beaucoup plus larges.\n\nUne troisème étape pour le fun.",
                    tags.subList(0, 4).stream().map(BaseEntity::getId).collect(Collectors.toSet()));

            tags.forEach(tag -> Repositories.tags().add(tag));
            ingredients.forEach(ingredient -> Repositories.ingredients().add(ingredient));
            Repositories.recipes().add(recipe);
        };
    }
}
