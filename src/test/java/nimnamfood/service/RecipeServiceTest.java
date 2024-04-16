package nimnamfood.service;

import nimnamfood.adapter.storage.FailedToUploadIllustrationException;
import nimnamfood.adapter.storage.MissingBlobException;
import nimnamfood.adapter.storage.StorageAdapter;
import nimnamfood.adapter.storage.BlobPublicUrl;
import nimnamfood.command.recipe.RecipeIngredientCommandPart;
import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;
import vtertre.ddd.MissingAggregateRootException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith({WithMemoryRepositories.class})
class RecipeServiceTest {
    StorageAdapter storageAdapter = Mockito.mock();

    @Test
    void importsAnIllustration() throws IOException {
        RecipeService service = new RecipeService(storageAdapter);
        MultipartFile file = Mockito.mock();
        InputStream fileStream = Mockito.mock();
        Mockito.when(file.getContentType()).thenReturn("content type");
        Mockito.when(file.getInputStream()).thenReturn(fileStream);
        UUID illustrationId = UUID.randomUUID();
        ArgumentCaptor<Map<String, String>> metadataCaptor = ArgumentCaptor.forClass(Map.class);

        service.importIllustration(illustrationId, file);

        Mockito.verify(storageAdapter, Mockito.times(1)).create(Mockito.eq("pending/" + illustrationId + ".webp"),
                Mockito.eq(fileStream), Mockito.eq("content type"), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).hasSize(1);
        assertThat(metadataCaptor.getValue().get("firebaseStorageDownloadTokens")).isEqualTo(illustrationId.toString());
    }

    @Test
    void throwsAnExceptionWhenUploadFails() throws IOException {
        RecipeService service = new RecipeService(storageAdapter);
        Mockito.doThrow(IOException.class).when(storageAdapter)
                .create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        assertThatExceptionOfType(FailedToUploadIllustrationException.class)
                .isThrownBy(() -> service.importIllustration(UUID.randomUUID(), Mockito.mock()));
    }

    @Test
    void movesThePendingIllustrationFileToLiveFolder() {
        RecipeService service = new RecipeService(storageAdapter);
        UUID illustrationId = UUID.randomUUID();
        Mockito.when(storageAdapter.exists("pending/" + illustrationId + ".webp")).thenReturn(true);
        Mockito.when(storageAdapter.copy("pending/" + illustrationId + ".webp", "live/recipes/" + illustrationId + ".webp")).thenReturn(true);

        service.activateIllustration(illustrationId);

        Mockito.verify(storageAdapter, Mockito.times(1))
                .copy("pending/" + illustrationId + ".webp", "live/recipes/" + illustrationId + ".webp");
        Mockito.verify(storageAdapter, Mockito.times(1))
                .delete("pending/" + illustrationId + ".webp");
    }

    @Test
    void throwsAnExceptionIfThePendingIllustrationBlobDoesNotExist() {
        RecipeService service = new RecipeService(storageAdapter);
        UUID illustrationId = UUID.randomUUID();
        Mockito.when(storageAdapter.exists("pending/" + illustrationId + ".webp")).thenReturn(false);

        assertThatExceptionOfType(MissingBlobException.class)
                .isThrownBy(() -> service.activateIllustration(illustrationId))
                .withMessage("BLOB_NOT_FOUND - " + illustrationId);
    }

    @Test
    void replacesAnIllustration() {
        RecipeService service = new RecipeService(storageAdapter);
        UUID currentIllustrationId = UUID.randomUUID();
        UUID newIllustrationId = UUID.randomUUID();
        Mockito.when(storageAdapter.exists("pending/" + newIllustrationId + ".webp")).thenReturn(true);
        Mockito.when(storageAdapter.copy("pending/" + newIllustrationId + ".webp", "live/recipes/" + newIllustrationId + ".webp")).thenReturn(true);

        service.replaceIllustration(currentIllustrationId, newIllustrationId);

        Mockito.verify(storageAdapter, Mockito.times(1))
                .copy("pending/" + newIllustrationId + ".webp", "live/recipes/" + newIllustrationId + ".webp");
        Mockito.verify(storageAdapter, Mockito.times(1))
                .delete("pending/" + newIllustrationId + ".webp");
        Mockito.verify(storageAdapter, Mockito.times(1))
                .delete("live/recipes/" + currentIllustrationId + ".webp");
    }

    @Test
    void deletesAnIllustration() {
        RecipeService service = new RecipeService(storageAdapter);
        UUID illustrationId = UUID.randomUUID();

        service.deleteIllustration(illustrationId);

        Mockito.verify(storageAdapter, Mockito.times(1))
                .delete("live/recipes/" + illustrationId + ".webp");
    }

    @Test
    void generatesAnIllustrationPublicUrl() {
        RecipeService service = new RecipeService(storageAdapter);
        UUID illustrationId = UUID.randomUUID();
        BlobPublicUrl url = new BlobPublicUrl("bucket", "folderA/folderB/blob");
        Mockito.when(storageAdapter.publicUrl("live/recipes/" + illustrationId + ".webp")).thenReturn(url);

        String result = service.illustrationUrl(illustrationId);

        assertThat(result).isEqualTo("https://firebasestorage.googleapis.com/v0/b/bucket/o/folderA%2FfolderB%2Fblob?alt=media&token=" + illustrationId);
    }

    @Test
    void extractsTheSetOfRecipeIngredientsFromACommandPart() {
        Ingredient ingredient = addIngredient(Ingredient.factory().create("ingredient", IngredientUnit.GRAM)._1);
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = ingredient.getId().toString();
            quantity = 2f;
            unit = IngredientUnit.GRAM;
            quantityFixed = false;
        }};

        Set<RecipeIngredient> result = RecipeService.recipeIngredientsFromCommand(Set.of(part));

        assertThat(result)
                .hasSize(1)
                .first()
                .matches(ri -> ri.getId() != null && ri.ingredientId().equals(ingredient.getId()) &&
                        ri.quantity() == 2f && ri.unit() == IngredientUnit.GRAM && !ri.quantityFixed());
    }

    @Test
    void throwsAnExceptionIfAnIngredientDoesNotExist() {
        RecipeIngredientCommandPart part = new RecipeIngredientCommandPart() {{
            ingredientId = UUID.randomUUID().toString();
            quantity = 2f;
            unit = IngredientUnit.GRAM;
            quantityFixed = false;
        }};

        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> RecipeService.recipeIngredientsFromCommand(Set.of(part)));
    }

    @Test
    void extractsTheSetOfTagAsUuids() {
        Tag tag1 = addTag(Tag.factory().create("1")._1);
        Tag tag2 = addTag(Tag.factory().create("2")._1);

        Set<UUID> result = RecipeService.tagIdsFromCommand(Set.of(tag1.getId().toString(), tag2.getId().toString()));

        assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(tag1.getId(), tag2.getId());
    }

    @Test
    void throwsAnExceptionIfATagDoesNotExist() {
        assertThatExceptionOfType(MissingAggregateRootException.class)
                .isThrownBy(() -> RecipeService.tagIdsFromCommand(Set.of(UUID.randomUUID().toString())));
    }

    private static Ingredient addIngredient(Ingredient ingredient) {
        Repositories.ingredients().add(ingredient);
        return ingredient;
    }

    private static Tag addTag(Tag tag) {
        Repositories.tags().add(tag);
        return tag;
    }
}