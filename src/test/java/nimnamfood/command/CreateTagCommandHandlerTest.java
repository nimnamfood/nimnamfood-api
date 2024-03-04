package nimnamfood.command;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class CreateTagCommandHandlerTest {
    @Test
    void addsTheTagToTheRepositoryAndReturnsUUID() {
        CreateTagCommandHandler handler = new CreateTagCommandHandler();
        CreateTagCommand command = new CreateTagCommand();
        command.name = "végé";

        UUID result = handler.execute(command);
        Tag tag = Repositories.tags().getAll().stream().findFirst().get();

        assertThat(result).isNotNull();
        assertThat(tag.getId()).isNotNull();
        assertThat(tag.getName()).isEqualTo("végé");
    }
}
