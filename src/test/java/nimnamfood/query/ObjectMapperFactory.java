package nimnamfood.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class ObjectMapperFactory {
    public static ObjectMapper withSnakeCasePropertyNamingStrategy() {
        return new ObjectMapper().setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
    }
}
