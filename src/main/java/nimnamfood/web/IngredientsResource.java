package nimnamfood.web;

import nimnamfood.command.CreateIngredientCommand;
import nimnamfood.query.ingredient.FindIngredients;
import nimnamfood.query.ingredient.model.IngredientSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vtertre.command.CommandBus;
import vtertre.query.QueryBus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@RestController
public class IngredientsResource {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public IngredientsResource(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @PostMapping("/ingredients")
    public Future<ResponseEntity<Map<String, UUID>>> create(@RequestBody CreateIngredientCommand command) {
        return this.commandBus
                .dispatch(command)
                .thenApply(result -> Collections.singletonMap("id", result))
                .thenApply(result -> new ResponseEntity<>(result, HttpStatus.CREATED));
    }

    @GetMapping("/ingredients")
    public Future<List<IngredientSummary>> get(@RequestParam(required = false, name = "q") String query) {
        return this.queryBus.dispatch(new FindIngredients(query));
    }
}
