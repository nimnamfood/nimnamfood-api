package nimnamfood.query;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GetHealthCheckHandlerTest {

    @Test
    void returnsOk() {
        Map<String, String> result = new GetHealthCheckHandler().execute(new GetHealthCheck());

        assertThat(result.get("result")).isEqualTo("ok");
    }
}
