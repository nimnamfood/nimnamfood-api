package nimnamfood.command.illustration;

import nimnamfood.command.illustration.validation.ValidFile;
import org.springframework.web.multipart.MultipartFile;
import vtertre.command.Command;

import java.util.UUID;

public class ImportIllustrationCommand implements Command<UUID> {
    @ValidFile(maxBytesSize = 150_000L)
    MultipartFile file;

    private ImportIllustrationCommand() {
    }

    public static ImportIllustrationCommand withFile(MultipartFile file) {
        final ImportIllustrationCommand command = new ImportIllustrationCommand();
        command.file = file;
        return command;
    }
}
