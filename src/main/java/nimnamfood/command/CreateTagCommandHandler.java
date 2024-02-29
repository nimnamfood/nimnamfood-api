package nimnamfood.command;

import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;

import java.util.UUID;

@Component
public class CreateTagCommandHandler implements CommandHandler<CreateTagCommand, UUID> {
    @Override
    public UUID execute(CreateTagCommand command) {
        final Tag tag = new Tag(command.name);
        Repositories.tags().add(tag);
        return tag.getId();
    }
}
