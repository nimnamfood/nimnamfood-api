package nimnamfood.web;

import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.query.ingredient.GetAllIngredientUnits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vtertre.query.QueryBus;

import java.util.Set;
import java.util.concurrent.Future;

@RestController
public class IngredientUnitsResource {
    private final QueryBus bus;

    @Autowired
    public IngredientUnitsResource(QueryBus bus) {
        this.bus = bus;
    }

    @GetMapping("/units")
    public Future<Set<IngredientUnit>> getAll() {
        return this.bus.dispatch(new GetAllIngredientUnits());
    }
}
