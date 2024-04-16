package nimnamfood.command.tag;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import nimnamfood.model.tag.TagCreated;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class CreateTagCommandHandlerTest {
    @Test
    void addsTheTagToTheRepositoryAndReturnsUUID() {
        CreateTagCommandHandler handler = new CreateTagCommandHandler();
        CreateTagCommand command = new CreateTagCommand();
        command.name = "végé";

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);
        Tag tag = Repositories.tags().get(result._1).get();

        assertThat(tag.getId()).isEqualTo(result._1);
        assertThat(tag.getName()).isEqualTo("végé");
        assertThat(result._2)
                .hasSize(1)
                .first()
                .asInstanceOf(InstanceOfAssertFactories.type(TagCreated.class))
                .satisfies(event -> {
                    assertThat(event.id()).isEqualTo(tag.getId());
                    assertThat(event.name()).isEqualTo("végé");
                });
    }
}
