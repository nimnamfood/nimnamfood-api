package nimnamfood.web;

import nimnamfood.command.recipe.CreateRecipeCommand;
import nimnamfood.query.recipe.FindRecipes;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vtertre.command.CommandBus;
import vtertre.query.QueryBus;

import java.util.*;
import java.util.concurrent.Future;

@RestController
public class RecipesResource {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    private final int MAX_SEARCH_RESULT = 15;

    @Autowired
    public RecipesResource(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @PostMapping("/recipes")
    public Future<ResponseEntity<Map<String, UUID>>> create(@RequestBody CreateRecipeCommand command) {
        return this.commandBus.dispatch(command)
                .thenApply(result -> Collections.singletonMap("id", result))
                .thenApply(result -> new ResponseEntity<>(result, HttpStatus.CREATED));
    }

    @GetMapping("/recipes")
    public Future<List<RecipeSearchSummary>> get(
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer limit) {
        return this.queryBus.dispatch(new FindRecipes(query, tags)
                .skip(skip != null ? Math.max(skip, 0) : 0)
                .limit(limit != null ? Math.clamp(limit, 0, MAX_SEARCH_RESULT) : 0));
    }
}
