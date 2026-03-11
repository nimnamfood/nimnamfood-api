package nimnamfood;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

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

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder
                .timeZone(TimeZone.getTimeZone("UTC"))
                .serializerByType(Instant.class, new TruncatedInstantJsonSerializer())
                .modules(new JavaTimeModule());
    }

    private static class TruncatedInstantJsonSerializer extends JsonSerializer<Instant> {
        final DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC);

        @Override
        public void serialize(Instant instant, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(this.formatter.format(instant));
        }
    }
}
