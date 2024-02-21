package nimnamfood.query;

import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.Collections;
import java.util.Map;

@Component
public class GetHealthCheckHandler implements QueryHandler<GetHealthCheck, Map<String, String>> {
    @Override
    public Map<String, String> execute(GetHealthCheck query) {
        return Collections.singletonMap("result", "ok");
    }
}
