package nimnamfood.command.tag;

import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import nimnamfood.model.tag.TagCreated;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.UUID;

@Component
public class CreateTagCommandHandler implements CommandHandler<CreateTagCommand, UUID> {
    @Override
    public Tuple<UUID, List<DomainEvent>> execute(CreateTagCommand command) {
        final Tuple<Tag, TagCreated> tuple = Tag.factory().create(command.name);
        Repositories.tags().add(tuple._1);
        return tuple.map(((tag, event) -> Tuple.of(tag.getId(), List.of(event))));
    }
}
