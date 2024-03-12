package nimnamfood.web;

import nimnamfood.query.recipe.GetRecipe;
import nimnamfood.query.recipe.model.RecipeSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vtertre.query.QueryBus;

import java.util.concurrent.Future;

@RestController
public class RecipeResource {
    private final QueryBus queryBus;

    @Autowired
    public RecipeResource(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/recipes/{stringUuid}")
    public Future<RecipeSummary> get(@PathVariable String stringUuid) {
        return this.queryBus.dispatch(new GetRecipe(stringUuid));
    }
}
