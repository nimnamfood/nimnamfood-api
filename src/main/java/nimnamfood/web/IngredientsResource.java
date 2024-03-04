package nimnamfood.web;

import nimnamfood.command.CreateIngredientCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vtertre.command.CommandBus;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@RestController
public class IngredientsResource {
    private final CommandBus commandBus;

    @Autowired
    public IngredientsResource(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping("/ingredients")
    public Future<Map<String, UUID>> create(@RequestBody CreateIngredientCommand command) {
        return this.commandBus
                .send(command)
                .thenApply(result -> Collections.singletonMap("id", result));
    }
}
