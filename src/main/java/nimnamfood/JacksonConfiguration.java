package nimnamfood;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfiguration {
    @Bean
    @Primary
    ObjectMapper springBootDefaultObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // As defined in JacksonAutoConfiguration class
        return builder.createXmlMapper(false).build();
    }

    @Bean
    @Qualifier("Jsonb")
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    ObjectMapper jsonbMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
    }
}
