package nimnamfood.command;

import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class CreateTagCommandHandler implements CommandHandler<CreateTagCommand, UUID> {
    @Override
    public Tuple<UUID, List<DomainEvent>> execute(CreateTagCommand command) {
        final Tag tag = new Tag(command.name);
        Repositories.tags().add(tag);
        return Tuple.of(tag.getId(), Collections.emptyList());
    }
}
