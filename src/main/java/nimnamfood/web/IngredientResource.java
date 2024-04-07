package nimnamfood.web;

import nimnamfood.command.ingredient.UpdateIngredientCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vtertre.command.CommandBus;

import java.util.UUID;
import java.util.concurrent.Future;

@RestController
public class IngredientResource {
    private final CommandBus commandBus;

    @Autowired
    public IngredientResource(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PutMapping("/ingredients/{stringUuid}")
    public Future<ResponseEntity<Void>> get(@PathVariable String stringUuid,
                                            @RequestBody UpdateIngredientCommand command) {
        return this.commandBus
                .dispatch(command.withId(UUID.fromString(stringUuid)))
                .thenApply(result -> new ResponseEntity<>(result, HttpStatus.NO_CONTENT));
    }
}
