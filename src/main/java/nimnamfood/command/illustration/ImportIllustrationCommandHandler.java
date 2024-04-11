package nimnamfood.command.illustration;

import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class ImportIllustrationCommandHandler implements CommandHandler<ImportIllustrationCommand, UUID> {
    private final RecipeService recipeService;

    @Autowired
    public ImportIllustrationCommandHandler(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Override
    public Tuple<UUID, List<DomainEvent>> execute(ImportIllustrationCommand command) {
        final UUID fileId = UUID.randomUUID();
        this.recipeService.importIllustration(fileId, command.file);
        return Tuple.of(fileId, Collections.emptyList());
    }
}
