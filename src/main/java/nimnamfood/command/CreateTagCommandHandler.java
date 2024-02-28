package nimnamfood.command;

import nimnamfood.model.Repositories;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;

import java.util.UUID;

@Component
public class CreateTagCommandHandler implements CommandHandler<CreateTagCommand, UUID> {
    @Override
    public UUID execute(CreateTagCommand command) {
        Repositories.tags().add(command.name);
        return UUID.randomUUID();
    }
}
