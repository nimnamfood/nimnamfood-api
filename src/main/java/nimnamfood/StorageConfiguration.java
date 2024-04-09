package nimnamfood;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class StorageConfiguration {
    @Bean
    @Lazy
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    Storage storage(@Value("${nimnamfood.storage.private-key-filename}") String privateKeyResourceFilename,
                    @Value("${nimnamfood.storage.project-id}") String projectId) throws IOException {
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(privateKeyResourceFilename);

        if (stream == null) {
            throw new RuntimeException("Failed to get service account key resource");
        }

        return StorageOptions
                .newBuilder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
