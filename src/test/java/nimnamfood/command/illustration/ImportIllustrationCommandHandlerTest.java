package nimnamfood.command.illustration;

import nimnamfood.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ImportIllustrationCommandHandlerTest {
    RecipeService recipeService = Mockito.mock();

    @Test
    void uploadsAndReturnsFileId() {
        MultipartFile file = Mockito.mock();
        ImportIllustrationCommandHandler handler = new ImportIllustrationCommandHandler(recipeService);
        ImportIllustrationCommand command = ImportIllustrationCommand.withFile(file);

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        assertThat(result._1).isNotNull();
        assertThat(result._2).isEmpty();
        Mockito.verify(recipeService, Mockito.times(1)).importIllustration(result._1, file);
    }
}