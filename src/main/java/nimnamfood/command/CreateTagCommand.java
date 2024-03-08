package nimnamfood.command;

import jakarta.validation.constraints.NotBlank;
import vtertre.command.Command;

import java.util.UUID;

public class CreateTagCommand implements Command<UUID> {
    @NotBlank
    public String name;
}
