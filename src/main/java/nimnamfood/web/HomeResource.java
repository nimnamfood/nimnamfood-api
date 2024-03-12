package nimnamfood.web;

import nimnamfood.query.GetHealthCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vtertre.query.QueryBus;

import java.util.Map;
import java.util.concurrent.Future;

@RestController
public class HomeResource {
    @Autowired
    private QueryBus queryBus;

    @GetMapping("/")
    public Future<Map<String, String>> checkHealth() {
        return this.queryBus.dispatch(new GetHealthCheck());
    }
}
