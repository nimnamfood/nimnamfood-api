package nimnamfood.infrastructure.repository.memory;

import nimnamfood.model.Repositories;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class WithMemoryRepositories implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Repositories.initialize(new MemoryRepositories());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        Repositories.initialize(null);
    }
}
