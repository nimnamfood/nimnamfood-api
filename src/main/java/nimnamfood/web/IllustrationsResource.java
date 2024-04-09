package nimnamfood.web;

import nimnamfood.command.illustration.ImportIllustrationCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vtertre.command.CommandBus;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@RestController
public class IllustrationsResource {
    private final CommandBus commandBus;

    @Autowired
    public IllustrationsResource(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping("/illustrations")
    public Future<ResponseEntity<Map<String, UUID>>> post(@RequestParam("file") MultipartFile file) {
        return this.commandBus
                .dispatch(ImportIllustrationCommand.withFile(file))
                .thenApply(result -> Collections.singletonMap("id", result))
                .thenApply(result -> new ResponseEntity<>(result, HttpStatus.CREATED));
    }
}
