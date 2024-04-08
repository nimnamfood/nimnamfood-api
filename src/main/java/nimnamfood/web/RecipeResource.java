package nimnamfood.web;

import nimnamfood.command.recipe.UpdateRecipeCommand;
import nimnamfood.query.recipe.GetRecipe;
import nimnamfood.query.recipe.model.RecipeSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vtertre.command.CommandBus;
import vtertre.query.QueryBus;

import java.util.UUID;
import java.util.concurrent.Future;

@RestController
public class RecipeResource {
    private final QueryBus queryBus;
    private final CommandBus commandBus;

    @Autowired
    public RecipeResource(QueryBus queryBus, CommandBus commandBus) {
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    @GetMapping("/recipes/{stringUuid}")
    public Future<RecipeSummary> get(@PathVariable String stringUuid) {
        return this.queryBus.dispatch(new GetRecipe(stringUuid));
    }

    @PutMapping("/recipes/{stringUuid}")
    public Future<ResponseEntity<Void>> put(@PathVariable String stringUuid,
                                            @RequestBody UpdateRecipeCommand command) {
        return this.commandBus
                .dispatch(command.withId(UUID.fromString(stringUuid)))
                .thenApply(result -> new ResponseEntity<>(result, HttpStatus.NO_CONTENT));
    }
}
